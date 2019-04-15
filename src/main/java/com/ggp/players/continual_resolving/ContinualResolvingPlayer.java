package com.ggp.players.continual_resolving;

import com.ggp.*;
import com.ggp.player_evaluators.IEvaluablePlayer;
import com.ggp.players.continual_resolving.cfrd.AugmentedIS.CFRDAugmentedCISWrapper;
import com.ggp.players.continual_resolving.cfrd.AugmentedIS.CFRDAugmentedGameDescriptionWrapper;
import com.ggp.players.continual_resolving.resolvers.ExternalCFRResolver;
import com.ggp.players.continual_resolving.utils.*;
import com.ggp.solvers.cfr.BaseCFRSolver;
import com.ggp.utils.PlayerHelpers;
import com.ggp.utils.random.RandomSampler;
import com.ggp.utils.strategy.InfoSetStrategy;
import com.ggp.utils.strategy.NormalizingInfoSetStrategyWrapper;
import com.ggp.utils.strategy.Strategy;
import com.ggp.utils.time.IterationTimer;

import java.util.*;

public class ContinualResolvingPlayer implements IEvaluablePlayer {
    public static class Factory implements IFactory {
        private static final long serialVersionUID = 1L;
        private ISubgameResolver.IFactory resolverFactory;
        private ArrayList<IEvaluablePlayer.IListener> resolvingListeners = new ArrayList<>();

        public Factory(ISubgameResolver.IFactory resolverFactory) {
            this.resolverFactory = resolverFactory;
        }

        public Factory(BaseCFRSolver.Factory cfrSolverFactory) {
            this(new ExternalCFRResolver.Factory(cfrSolverFactory));
        }

        @Override
        public ContinualResolvingPlayer create(IGameDescription game, int role) {
            ContinualResolvingPlayer ret = new ContinualResolvingPlayer(role, game, resolverFactory);
            for (IListener l: resolvingListeners) {
                if (l != null) ret.registerResolvingListener(l);
            }
            return ret;
        }

        @Override
        public void registerResolvingListener(IListener listener) {
            if (listener != null) resolvingListeners.add(listener);
        }

        @Override
        public void unregisterResolvingListener(IListener listener) {
            if (listener != null) resolvingListeners.remove(listener);
        }

        @Override
        public String getConfigString() {
            return "ContinualResolving{" +
                        resolverFactory.getConfigString() +
                    '}';
        }
    }

    private int id;
    private int opponentId;
    private CISRange range;
    private IInformationSet hiddenInfo;
    private HashMap<IInformationSet, Double> opponentCFV;
    private HashMap<ICompleteInformationState, Double> reachProbs;
    private double cfvNorm;
    private double reachProbsNorm;
    private IGameDescription gameDesc;
    private ArrayList<IEvaluablePlayer.IListener> resolvingListeners = new ArrayList<>();
    private ISubgameResolver.IFactory resolverFactory;
    private SubgameMap subgameMap;
    private RandomSampler sampler = new RandomSampler();
    private ISubgameResolver currentResolver = null;
    private IInfoSetStrategy isStrat = null;

    private ContinualResolvingPlayer(ContinualResolvingPlayer other) {
        this.id = other.id;
        this.opponentId = other.opponentId;
        this.range = other.range;
        this.hiddenInfo = other.hiddenInfo;
        this.opponentCFV = other.opponentCFV != null ? new HashMap<>(other.opponentCFV) : null;
        this.reachProbs = other.reachProbs != null ? new HashMap<>(other.reachProbs) : null;
        this.cfvNorm = other.cfvNorm;
        this.reachProbsNorm = other.reachProbsNorm;
        this.gameDesc = other.gameDesc;
        this.resolvingListeners = new ArrayList<>(other.resolvingListeners);
        this.resolverFactory = other.resolverFactory;
        this.subgameMap = other.subgameMap;
        this.currentResolver = other.currentResolver == null ? null : other.currentResolver.copy(this.resolvingListeners);
        this.isStrat = other.isStrat == null ? null : new InfoSetStrategy(other.isStrat);
    }

    public ContinualResolvingPlayer(int id, IGameDescription gameDesc, ISubgameResolver.IFactory resolverFactory) {
        this.id = id;
        this.opponentId = PlayerHelpers.getOpponentId(id);
        gameDesc = new CFRDAugmentedGameDescriptionWrapper(gameDesc, opponentId);
        this.gameDesc = gameDesc;
        IInformationSet initialSet = this.gameDesc.getInitialInformationSet(id);
        CFRDAugmentedCISWrapper initialState = (CFRDAugmentedCISWrapper) this.gameDesc.getInitialState();
        hiddenInfo = initialSet;
        range = new CISRange(initialState);
        cfvNorm = 1;
        reachProbsNorm = 1;
        this.resolverFactory = resolverFactory;
    }

    @Override
    public void registerResolvingListener(IListener listener) {
        if (listener != null) resolvingListeners.add(listener);
    }

    @Override
    public void unregisterResolvingListener(IListener listener) {
        if (listener != null) resolvingListeners.remove(listener);
    }

    private ISubgameResolver createResolver() {
        return resolverFactory.create(id, hiddenInfo, range, opponentCFV, cfvNorm, resolvingListeners);
    }

    @Override
    public void init(long timeoutMillis) {
        IterationTimer timer = new IterationTimer(timeoutMillis);
        timer.start();
        currentResolver = createResolver();
        currentResolver.init(gameDesc.getInitialState(), timer);
    }

    @Override
    public int getRole() {
        return id;
    }

    @Override
    public void computeStrategy(long timeoutMillis) {
        IterationTimer timer = new IterationTimer(timeoutMillis);
        timer.start();
        if (subgameMap != null) {
            Set<ICompleteInformationState> subgame = subgameMap.getSubgame(hiddenInfo);
            if (subgame != null) {
                // entering new subgame
                range = new CISRange(subgameMap.getSubgame(hiddenInfo), reachProbs, reachProbsNorm);
                currentResolver = createResolver();
            }
        }

        ISubgameResolver.ActResult res = currentResolver.act(timer, hiddenInfo);
        subgameMap = res.subgameMap;
        isStrat = res.cumulativeStrategy.getInfoSetStrategy(hiddenInfo);
        opponentCFV = res.nextOpponentCFV;
        reachProbs = res.nextRange;
        cfvNorm = res.cfvNorm;
        reachProbsNorm = res.rangeNorm;
    }

    @Override
    public ContinualResolvingPlayer copy() {
        return new ContinualResolvingPlayer(this);
    }

    private IAction act(IAction forcedAction, long timeoutMillis) {
        computeStrategy(timeoutMillis);
        IAction selectedAction;
        if (forcedAction == null) {
            selectedAction = PlayerHelpers.sampleAction(sampler, hiddenInfo, new NormalizingInfoSetStrategyWrapper(isStrat));
        } else {
            selectedAction = forcedAction;
        }

        actWithPrecomputedStrategy(selectedAction);
        return selectedAction;
    }

    @Override
    public void actWithPrecomputedStrategy(IAction selectedAction) {
        hiddenInfo = hiddenInfo.next(selectedAction);
        isStrat = null;
    }

    @Override
    public void forceAction(IAction forcedAction, long timeoutMillis) {
        act(forcedAction, timeoutMillis);
    }

    @Override
    public IAction act(long timeoutMillis) {
        return act(null, timeoutMillis);
    }

    @Override
    public void receivePercepts(IPercept percept) {
        hiddenInfo = hiddenInfo.applyPercept(percept);
        isStrat = null;
    }

    @Override
    public IStrategy getNormalizedSubgameStrategy() {
        if (currentResolver == null) return new Strategy();
        return currentResolver.getResolvingInfo().getNormalizedSubgameStrategy();
    }
}

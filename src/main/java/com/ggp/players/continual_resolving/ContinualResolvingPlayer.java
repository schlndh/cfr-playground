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
import com.ggp.utils.strategy.NormalizingStrategyWrapper;
import com.ggp.utils.strategy.RestrictedStrategy;
import com.ggp.utils.time.IterationTimer;

import java.util.*;

public class ContinualResolvingPlayer implements IEvaluablePlayer {
    public static class Factory implements IEvaluablePlayer.IFactory {
        private static final long serialVersionUID = 1L;
        private ISubgameResolver.Factory resolverFactory;
        private ArrayList<IEvaluablePlayer.IListener> resolvingListeners = new ArrayList<>();

        public Factory(ISubgameResolver.Factory resolverFactory) {
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
    private long norm;
    private IGameDescription gameDesc;
    private IAction myLastAction;
    private IStrategy lastCumulativeStrategy;
    private ArrayList<IEvaluablePlayer.IListener> resolvingListeners = new ArrayList<>();
    private ISubgameResolver.Factory resolverFactory;
    private SubgameMap subgameMap;
    private RandomSampler sampler = new RandomSampler();
    private ISubgameResolver lastResolver = null;
    private int subgameActDepth = 1;
    private HashSet<IInformationSet> subgameActingIs = null;

    private ContinualResolvingPlayer(ContinualResolvingPlayer other) {
        this.id = other.id;
        this.opponentId = other.opponentId;
        this.range = other.range;
        this.hiddenInfo = other.hiddenInfo;
        this.opponentCFV = new HashMap<>(other.opponentCFV);
        this.reachProbs = other.reachProbs != null ? new HashMap<>(other.reachProbs) : null;
        this.norm = other.norm;
        this.gameDesc = other.gameDesc;
        this.myLastAction = other.myLastAction;
        this.lastCumulativeStrategy = other.lastCumulativeStrategy;
        this.resolvingListeners = new ArrayList<>(other.resolvingListeners);
        this.resolverFactory = other.resolverFactory;
        this.subgameMap = other.subgameMap;
        this.lastResolver = other.lastResolver == null ? null : other.lastResolver.copy(this.resolvingListeners);
        this.subgameActDepth = other.subgameActDepth;
        this.subgameActingIs = null; // will re-create automatically if necessary
    }

    public ContinualResolvingPlayer(int id, IGameDescription gameDesc, ISubgameResolver.Factory resolverFactory) {
        this.id = id;
        this.opponentId = PlayerHelpers.getOpponentId(id);
        gameDesc = new CFRDAugmentedGameDescriptionWrapper(gameDesc, opponentId);
        this.gameDesc = gameDesc;
        IInformationSet initialSet = this.gameDesc.getInitialInformationSet(id);
        CFRDAugmentedCISWrapper initialState = (CFRDAugmentedCISWrapper) this.gameDesc.getInitialState();
        hiddenInfo = initialSet;
        IInformationSet initialOpponentSet = initialState.getOpponentsAugmentedIS();
        range = new CISRange(initialState);
        opponentCFV = new HashMap<>(1);
        opponentCFV.put(initialOpponentSet, 0d);
        norm = 1;
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
        return resolverFactory.create(id, hiddenInfo, range, opponentCFV, norm, resolvingListeners);
    }

    @Override
    public void init(long timeoutMillis) {
        IterationTimer timer = new IterationTimer(timeoutMillis);
        timer.start();
        ISubgameResolver r = createResolver();
        r.init(gameDesc.getInitialState(), timer);
        lastResolver = r;
    }

    @Override
    public int getRole() {
        return id;
    }

    @Override
    public void computeStrategy(long timeoutMillis) {
        IterationTimer timer = new IterationTimer(timeoutMillis);
        timer.start();
        ISubgameResolver r = lastResolver;
        if (subgameMap != null) {
            Set<ICompleteInformationState> subgame = subgameMap.getSubgame(hiddenInfo);
            if (subgame != null) {
                // entering new subgame
                range = new CISRange(subgameMap.getSubgame(hiddenInfo), reachProbs, norm);
                r = createResolver();
                subgameActDepth = 1;
                subgameActingIs = null;
            }
        }

        ISubgameResolver.ActResult res = r.act(timer);
        lastCumulativeStrategy = res.cumulativeStrategy;
        subgameMap = res.subgameMap;
        opponentCFV = res.nextOpponentCFV;
        reachProbs = res.nextRange;
        norm = res.norm;
    }

    @Override
    public ContinualResolvingPlayer copy() {
        return new ContinualResolvingPlayer(this);
    }

    private IAction act(IAction forcedAction, long timeoutMillis) {
        computeStrategy(timeoutMillis);
        IAction selectedAction;
        if (forcedAction == null) {
            selectedAction = PlayerHelpers.sampleAction(sampler, hiddenInfo, new NormalizingStrategyWrapper(lastCumulativeStrategy));
        } else {
            selectedAction = forcedAction;
        }

        actWithPrecomputedStrategy(selectedAction);
        return selectedAction;
    }

    @Override
    public void actWithPrecomputedStrategy(IAction selectedAction) {
        myLastAction = selectedAction;
        hiddenInfo = hiddenInfo.next(selectedAction);
        subgameActDepth++;
        subgameActingIs = null;
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
    }

    private void findMySubgameTurn(ICompleteInformationState s, int turns) {
        if (s.isTerminal()) return;
        if (s.getActingPlayerId() == id) {
            turns++;
            if (turns == subgameActDepth) {
                subgameActingIs.add(s.getInfoSetForActingPlayer());
                return;
            }
        }
        for (IAction a: s.getLegalActions()) {
            findMySubgameTurn(s.next(a), turns);
        }
    }

    @Override
    public IStrategy getNormalizedSubgameStrategy() {
        if (subgameActingIs == null) {
            subgameActingIs = new HashSet<>();
            for (ICompleteInformationState s: range.getPossibleStates()) {
                findMySubgameTurn(s, 0);
            }
        }
        return new NormalizingStrategyWrapper(new RestrictedStrategy(lastCumulativeStrategy, subgameActingIs));
    }
}

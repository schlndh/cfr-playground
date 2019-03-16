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
    private IGameDescription gameDesc;
    private IAction myLastAction;
    private IStrategy lastCumulativeStrategy;
    private ArrayList<IEvaluablePlayer.IListener> resolvingListeners = new ArrayList<>();
    private ISubgameResolver.Factory resolverFactory;
    private SubgameMap subgameMap;
    private NextRangeTree nrt;
    private RandomSampler sampler = new RandomSampler();
    private ISubgameResolver initResolver = null;

    private ContinualResolvingPlayer(ContinualResolvingPlayer other) {
        this.id = other.id;
        this.opponentId = other.opponentId;
        this.range = other.range;
        this.hiddenInfo = other.hiddenInfo;
        this.opponentCFV = other.opponentCFV;
        this.gameDesc = other.gameDesc;
        this.myLastAction = other.myLastAction;
        this.lastCumulativeStrategy = other.lastCumulativeStrategy;
        this.resolvingListeners = new ArrayList<>(other.resolvingListeners);
        this.resolverFactory = other.resolverFactory;
        this.subgameMap = other.subgameMap;
        this.nrt = other.nrt;
        this.initResolver = other.initResolver == null ? null : other.initResolver.copy(this.resolvingListeners);
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
        return resolverFactory.create(id, hiddenInfo, range, opponentCFV, resolvingListeners);
    }

    @Override
    public void init(long timeoutMillis) {
        IterationTimer timer = new IterationTimer(timeoutMillis);
        timer.start();
        ISubgameResolver r = createResolver();
        r.init(gameDesc.getInitialState(), timer);
        initResolver = r;
    }

    @Override
    public int getRole() {
        return id;
    }

    @Override
    public void computeStrategy(long timeoutMillis) {
        IterationTimer timer = new IterationTimer(timeoutMillis);
        timer.start();
        ISubgameResolver r = initResolver;
        if (r == null) {
            range = new CISRange(subgameMap.getSubgame(hiddenInfo), nrt, lastCumulativeStrategy);
            r = createResolver();
        }

        ISubgameResolver.ActResult res = r.act(timer);
        lastCumulativeStrategy = res.cumulativeStrategy;
        subgameMap = res.subgameMap;
        nrt = res.nrt;
        opponentCFV = res.nextOpponentCFV;
        initResolver = null;
    }

    @Override
    public ContinualResolvingPlayer copy() {
        return new ContinualResolvingPlayer(this);
    }

    private IAction act(IAction forcedAction, long timeoutMillis) {
        computeStrategy(timeoutMillis);
        IAction selectedAction;
        if (forcedAction == null) {
            selectedAction = PlayerHelpers.sampleAction(sampler, hiddenInfo, lastCumulativeStrategy);
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

    @Override
    public IStrategy getNormalizedSubgameStrategy() {
        return lastCumulativeStrategy;
    }
}
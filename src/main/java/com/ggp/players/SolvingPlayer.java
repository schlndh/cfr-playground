package com.ggp.players;

import com.ggp.*;
import com.ggp.player_evaluators.IEvaluablePlayer;
import com.ggp.players.deepstack.trackers.IGameTraversalTracker;
import com.ggp.players.deepstack.trackers.SimpleTracker;
import com.ggp.solvers.cfr.BaseCFRSolver;
import com.ggp.utils.random.RandomSampler;
import com.ggp.utils.strategy.NormalizingInfoSetStrategyWrapper;
import com.ggp.utils.strategy.NormalizingStrategyWrapper;
import com.ggp.utils.time.IterationTimer;

import java.util.ArrayList;
import java.util.List;

public class SolvingPlayer implements IEvaluablePlayer {
    public static class Factory implements IEvaluablePlayer.IFactory {
        private BaseCFRSolver.Factory cfrSolverFactory;
        private ArrayList<IListener> resolvingListeners = new ArrayList<>();

        public Factory(BaseCFRSolver.Factory cfrSolverFactory) {
            this.cfrSolverFactory = cfrSolverFactory;
        }

        @Override
        public SolvingPlayer create(IGameDescription game, int role) {
            SolvingPlayer ret = new SolvingPlayer(cfrSolverFactory.create(null), game, role);
            for (IListener l: resolvingListeners) {
                if (l != null) ret.registerResolvingListener(l);
            }
            return ret;
        }

        @Override
        public String toString() {
            return "SolvingPlayer{" +
                        cfrSolverFactory.getConfigString() +
                    '}';
        }

        @Override
        public String getConfigString() {
            return toString();
        }

        @Override
        public void registerResolvingListener(IListener listener) {
            if (listener != null) resolvingListeners.add(listener);
        }

        @Override
        public void unregisterResolvingListener(IListener listener) {
            if (listener != null) resolvingListeners.remove(listener);
        }
    }

    private class SolvingInfo implements IEvaluablePlayer.IResolvingInfo {
        @Override
        public IStrategy getNormalizedSubgameStrategy() {
            //TODO: limit this to current subgame
            return new NormalizingStrategyWrapper(cfrSolver.getCumulativeStrat());
        }

        @Override
        public IInformationSet getCurrentInfoSet() {
            return currentInfoSet;
        }

        @Override
        public long getVisitedStatesInCurrentResolving() {
            return cfrSolver.getVisitedStates() - lastVisitedStates;
        }
    }

    private BaseCFRSolver cfrSolver;
    private IInformationSet currentInfoSet;
    private IGameTraversalTracker rootTracker;
    private final int role;
    private final RandomSampler sampler = new RandomSampler();
    private ArrayList<IListener> resolvingListeners = new ArrayList<>();
    private SolvingInfo resInfo = new SolvingInfo();
    private long lastVisitedStates = 0;

    public SolvingPlayer(BaseCFRSolver cfrSolver, IGameDescription gameDesc, int role) {
        this.cfrSolver = cfrSolver;
        this.currentInfoSet = gameDesc.getInitialInformationSet(role);
        this.rootTracker = SimpleTracker.createRoot(gameDesc.getInitialState());
        this.role = role;
    }

    protected SolvingPlayer(SolvingPlayer player) {
        this.cfrSolver = player.cfrSolver.copy(null);
        this.currentInfoSet = player.currentInfoSet;
        this.rootTracker = player.rootTracker;
        this.role = player.role;
        this.resolvingListeners = new ArrayList<>(player.resolvingListeners);
        this.lastVisitedStates = player.lastVisitedStates;
    }

    @Override
    public void init(long timeoutMillis) {
        computeStrategy(timeoutMillis);
        resolvingListeners.forEach(listener -> listener.initEnd(resInfo));
    }

    private void computeStrategy(IterationTimer timer) {
        resolvingListeners.forEach(listener -> listener.resolvingStart(resInfo));
        while (timer.canDoAnotherIteration()) {
            timer.startIteration();
            cfrSolver.runIteration(rootTracker);
            timer.endIteration();
            timer.stop();
            resolvingListeners.forEach(listener -> listener.resolvingIterationEnd(resInfo));
            timer.start();
        }
        resolvingListeners.forEach(listener -> listener.resolvingEnd(resInfo));
    }

    @Override
    public void computeStrategy(long timeoutMillis) {
        IterationTimer timer = new IterationTimer(timeoutMillis);
        timer.start();
        computeStrategy(timer);
    }

    @Override
    public void actWithPrecomputedStrategy(IAction a) {
        currentInfoSet = currentInfoSet.next(a);
    }

    @Override
    public IAction act(long timeoutMillis) {
        computeStrategy(timeoutMillis);
        List<IAction> legalActions = currentInfoSet.getLegalActions();
        IInfoSetStrategy isStrat = new NormalizingInfoSetStrategyWrapper(cfrSolver.getCumulativeStrat().getInfoSetStrategy(currentInfoSet));
        int actionIdx = sampler.selectIdx(legalActions.size(), idx -> isStrat.getProbability(idx)).getResult();
        IAction a = legalActions.get(actionIdx);
        actWithPrecomputedStrategy(a);
        return a;
    }

    @Override
    public void forceAction(IAction a, long timeoutMillis) {
        computeStrategy(timeoutMillis);
        actWithPrecomputedStrategy(a);
    }

    @Override
    public int getRole() {
        return role;
    }

    @Override
    public void receivePercepts(IPercept percept) {
        currentInfoSet = currentInfoSet.applyPercept(percept);
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
    public SolvingPlayer copy() {
        return new SolvingPlayer(this);
    }

    @Override
    public IStrategy getNormalizedSubgameStrategy() {
        return resInfo.getNormalizedSubgameStrategy();
    }
}

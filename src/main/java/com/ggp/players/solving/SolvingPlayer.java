package com.ggp.players.solving;

import com.ggp.*;
import com.ggp.player_evaluators.IEvaluablePlayer;
import com.ggp.players.continual_resolving.trackers.IGameTraversalTracker;
import com.ggp.players.continual_resolving.trackers.SimpleTracker;
import com.ggp.solvers.cfr.BaseCFRSolver;
import com.ggp.solvers.cfr.ITargetableSolver;
import com.ggp.solvers.cfr.targeting.InfoSetSearchTargeting;
import com.ggp.utils.ActionIdxWrapper;
import com.ggp.utils.ObjectTree;
import com.ggp.utils.random.RandomSampler;
import com.ggp.utils.strategy.NormalizingInfoSetStrategyWrapper;
import com.ggp.utils.strategy.NormalizingStrategyWrapper;
import com.ggp.utils.strategy.PlayerLimitedStrategy;
import com.ggp.utils.time.IterationTimer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class SolvingPlayer implements IEvaluablePlayer {
    public static class Factory implements IEvaluablePlayer.IFactory {
        private BaseCFRSolver.Factory cfrSolverFactory;
        private ArrayList<IListener> resolvingListeners = new ArrayList<>();

        public Factory(BaseCFRSolver.Factory cfrSolverFactory) {
            if (cfrSolverFactory == null) {
                throw new IllegalArgumentException("CFR solver factory can't be null!");
            }
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
            return new NormalizingStrategyWrapper(new IStrategy() {
                    @Override
                    public Iterable<IInformationSet> getDefinedInformationSets() {
                        return subgame;
                    }

                    @Override
                    public boolean isDefined(IInformationSet is) {
                        return subgame.contains(is);
                    }

                    @Override
                    public IInfoSetStrategy getInfoSetStrategy(IInformationSet is) {
                        return cfrSolver.getCumulativeStrat().getInfoSetStrategy(is);
                    }
                }
            );
        }

        @Override
        public IStrategy getNormalizedCompleteStrategy() {
            return new NormalizingStrategyWrapper(new PlayerLimitedStrategy(cfrSolver.getCumulativeStrat(), role));
        }

        @Override
        public IInformationSet getCurrentInfoSet() {
            return currentInfoSet;
        }

        @Override
        public long getVisitedStatesInCurrentResolving() {
            return cfrSolver.getVisitedStates() + directlyVisitedStates;
        }
    }

    private BaseCFRSolver cfrSolver;
    private IInformationSet currentInfoSet;
    private IGameTraversalTracker rootTracker;
    private final int role;
    private final RandomSampler sampler = new RandomSampler();
    private ArrayList<IListener> resolvingListeners = new ArrayList<>();
    private SolvingInfo resInfo = new SolvingInfo();
    private long directlyVisitedStates = 0;
    private int resolves = -1;
    private HashSet<IInformationSet> subgame = new HashSet<>();
    private ObjectTree<ActionIdxWrapper> currentPathTree = new ObjectTree<>();
    private final boolean useISTargeting;

    public SolvingPlayer(BaseCFRSolver cfrSolver, IGameDescription gameDesc, int role) {
        this.cfrSolver = cfrSolver;
        this.currentInfoSet = gameDesc.getInitialInformationSet(role);
        this.rootTracker = SimpleTracker.createRoot(gameDesc.getInitialState());
        this.role = role;
        if (cfrSolver instanceof ITargetableSolver) {
            this.useISTargeting = ((ITargetableSolver) cfrSolver).wantsTargeting();
        } else {
            this.useISTargeting = false;
        }

    }

    protected SolvingPlayer(SolvingPlayer player) {
        this.cfrSolver = player.cfrSolver.copy(null);
        this.currentInfoSet = player.currentInfoSet;
        this.rootTracker = player.rootTracker;
        this.role = player.role;
        this.resolvingListeners = new ArrayList<>(player.resolvingListeners);
        this.resolves = player.resolves;
        this.subgame = new HashSet<>(player.subgame);
        this.useISTargeting = player.useISTargeting;
        this.directlyVisitedStates = player.directlyVisitedStates;
    }

    private void fillSubgame() {
        subgame = new HashSet<>();
        subgame.add(currentInfoSet);

        if (resolvingListeners.isEmpty() && !useISTargeting) return;
        fillSubgame(rootTracker.getCurrentState(), 0, new ArrayList<>());
        if (useISTargeting) {
            ITargetableSolver s = (ITargetableSolver) cfrSolver;
            s.setTargeting(new InfoSetSearchTargeting(currentPathTree));
        }
    }

    private void fillSubgame(ICompleteInformationState s, int myActions, ArrayList<ActionIdxWrapper> actionPath) {
        directlyVisitedStates++;
        if (s.isTerminal()) return;
        int myNextActions = myActions;
        if (s.getActingPlayerId() == role) {
            myNextActions++;
            if (myActions == resolves) {
                if (s.getInfoSetForActingPlayer().equals(currentInfoSet)) {
                    if (useISTargeting) currentPathTree.addPath(actionPath);
                    // current IS is already added to subgame
                }
                // prune all infoSets not below my current IS when targeting is used
                else if (useISTargeting) {
                    return;
                } else {
                    // IS targeting is not used and this IS is at the same action-depth as the current IS -> add to subgame
                    subgame.add(s.getInfoSetForActingPlayer());
                    return;
                }
            }

            if (myActions == resolves + 1) {
                // IS targeting is used and this IS is "under" my current IS -> add to subgame
                subgame.add(s.getInfoSetForActingPlayer());
                return;
            }
        }
        int actionIdx = 0;
        for (IAction a: s.getLegalActions()) {
            actionPath.add(new ActionIdxWrapper(a, actionIdx));
            fillSubgame(s.next(a), myNextActions, actionPath);
            actionPath.remove(actionPath.size() - 1);
            actionIdx++;
        }
    }

    @Override
    public void init(long timeoutMillis) {
        computeStrategy(timeoutMillis);
        resolvingListeners.forEach(listener -> listener.initEnd(resInfo));
    }

    private void runWithPausedTimer(IterationTimer timer, Runnable fn) {
        timer.stop();
        fn.run();
        timer.start();
    }

    private void computeStrategy(IterationTimer timer) {
        directlyVisitedStates = 0;
        cfrSolver.clearVisitedStates();
        runWithPausedTimer(timer, () -> resolvingListeners.forEach(listener -> listener.resolvingStart(resInfo)));
        fillSubgame();
        resolves++;
        do {
            timer.startIteration();
            cfrSolver.runIteration(rootTracker);
            timer.endIteration();
            runWithPausedTimer(timer, () -> resolvingListeners.forEach(listener -> listener.resolvingIterationEnd(resInfo)));
        } while (timer.canDoAnotherIteration());
        runWithPausedTimer(timer, () -> resolvingListeners.forEach(listener -> listener.resolvingEnd(resInfo)));
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

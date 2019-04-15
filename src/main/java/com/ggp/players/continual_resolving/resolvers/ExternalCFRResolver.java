package com.ggp.players.continual_resolving.resolvers;

import com.ggp.*;
import com.ggp.player_evaluators.IEvaluablePlayer;
import com.ggp.players.continual_resolving.ISubgameResolver;
import com.ggp.players.continual_resolving.cfrd.AugmentedIS.CFRDAugmentedCISWrapper;
import com.ggp.players.continual_resolving.cfrd.CFRDGadgetRoot;
import com.ggp.players.continual_resolving.trackers.CFRDTracker;
import com.ggp.players.continual_resolving.trackers.IGameTraversalTracker;
import com.ggp.players.continual_resolving.utils.*;
import com.ggp.solvers.cfr.BaseCFRSolver;
import com.ggp.solvers.cfr.ITargetableSolver;
import com.ggp.solvers.cfr.targeting.InfoSetSearchTargeting;
import com.ggp.utils.ActionIdxWrapper;
import com.ggp.utils.ObjectTree;
import com.ggp.utils.PlayerHelpers;
import com.ggp.utils.strategy.NormalizingStrategyWrapper;
import com.ggp.utils.strategy.PlayerLimitedStrategy;
import com.ggp.utils.strategy.RestrictedStrategy;
import com.ggp.utils.time.IterationTimer;

import java.util.*;

public class ExternalCFRResolver implements ISubgameResolver {
    public static class Factory implements IFactory {
        private BaseCFRSolver.Factory solverFactory;

        public Factory(BaseCFRSolver.Factory solverFactory) {
            this.solverFactory = solverFactory;
        }

        @Override
        public ISubgameResolver create(int myId, IInformationSet hiddenInfo, CISRange myRange, HashMap<IInformationSet, Double> opponentCFV,
                                       double opponentCfvNorm, ArrayList<IEvaluablePlayer.IListener> resolvingListeners)
        {
            return new ExternalCFRResolver(myId, hiddenInfo, myRange, opponentCFV, opponentCfvNorm, resolvingListeners, solverFactory);
        }

        @Override
        public String getConfigString() {
            return solverFactory.getConfigString();
        }
    }

    private final int myId;
    private IInformationSet hiddenInfo;
    private CISRange range;
    private HashMap<IInformationSet, Double> opponentCFV;
    private double opponentCFVNorm;
    private List<IEvaluablePlayer.IListener> resolvingListeners;
    private ResolvingInfo resInfo = new ResolvingInfo();
    private final int opponentId;

    private BaseCFRSolver.Factory solverFactory;
    private SubgameMap subgameMap;
    private HashMap<IInformationSet, Double> nextOpponentCFV = new HashMap<>();
    private HashMap<ICompleteInformationState, Double> nextReachProbs = new HashMap<>();
    private IStrategy cummulativeStrategy;
    private BaseCFRSolver cfrSolver = null;
    private long visitedStates = 0;
    private long iters = 0;
    private CFRDTracker rootTracker = null;
    private int subgameActDepth = 0;
    private HashSet<IInformationSet> subgameActingIs = null;
    private boolean useISTargeting = false;

    private class ResolvingInfo implements IEvaluablePlayer.IResolvingInfo {
        @Override
        public IStrategy getNormalizedSubgameStrategy() {
            if (subgameActingIs == null) {
                subgameActingIs = new HashSet<>();
                findMySubgameTurn(rootTracker, 0);
            }
            return new NormalizingStrategyWrapper(new RestrictedStrategy(cummulativeStrategy, subgameActingIs));
        }


        @Override
        public IStrategy getNormalizedCompleteStrategy() {
            if (rootTracker == null || rootTracker.getCurrentState().getClass() == CFRDGadgetRoot.class) return null;
            return new NormalizingStrategyWrapper(new PlayerLimitedStrategy(cummulativeStrategy, myId));
        }

        @Override
        public IInformationSet getCurrentInfoSet() {
            return hiddenInfo;
        }

        @Override
        public long getVisitedStatesInCurrentResolving() {
            if (cfrSolver == null) return visitedStates;
            return visitedStates + cfrSolver.getVisitedStates();
        }
    }

    public ExternalCFRResolver(int myId, IInformationSet hiddenInfo, CISRange range, HashMap<IInformationSet, Double> opponentCFV,
                               double opponentCFVNorm, ArrayList<IEvaluablePlayer.IListener> resolvingListeners,
                               BaseCFRSolver.Factory solverFactory)
    {
        this.myId = myId;
        this.hiddenInfo = hiddenInfo;
        this.range = range;
        this.opponentCFV = opponentCFV;
        this.opponentCFVNorm = opponentCFVNorm;
        this.resolvingListeners = resolvingListeners;
        if (this.resolvingListeners == null) this.resolvingListeners = new ArrayList<>();
        this.opponentId = PlayerHelpers.getOpponentId(myId);
        this.solverFactory = solverFactory;
        this.subgameMap = new SubgameMap(opponentId);
    }

    private ExternalCFRResolver(ExternalCFRResolver other) {
        this.myId = other.myId;
        this.hiddenInfo = other.hiddenInfo;
        this.range = other.range;
        this.opponentCFV = other.opponentCFV != null ? new HashMap<>(other.opponentCFV) : null;
        this.opponentCFVNorm = other.opponentCFVNorm;
        this.resolvingListeners = null;
        this.opponentId = other.opponentId;
        this.solverFactory = other.solverFactory;
        this.subgameMap = other.subgameMap;
        this.nextOpponentCFV = new HashMap<>(other.nextOpponentCFV);
        this.nextReachProbs = new HashMap<>(other.nextReachProbs);
        this.cfrSolver = other.cfrSolver.copy();
        this.cummulativeStrategy = this.cfrSolver.getCumulativeStrat();
        this.visitedStates = other.visitedStates;
        this.iters = other.iters;
        this.rootTracker = other.rootTracker;
        this.subgameActDepth = other.subgameActDepth;
        this.subgameActingIs = null; // will re-create automatically if necessary
        this.useISTargeting = other.useISTargeting;
    }

    private BaseCFRSolver createSolver() {
        cfrSolver = solverFactory.create(null);

        cfrSolver.registerListener(new BaseCFRSolver.IListener() {
            @Override
            public void enteringState(IGameTraversalTracker tracker, BaseCFRSolver.Info info) {
            }

            @Override
            public void leavingState(IGameTraversalTracker t, BaseCFRSolver.Info info, double p1Utility) {
                CFRDTracker tracker = (CFRDTracker) t;
                if (tracker.wasMyFirstTurnReached() && tracker.isSubgameRoot()) {
                    double probWithoutOpponent = info.rndProb * PlayerHelpers.selectByPlayerId(myId, info.reachProb1, info.reachProb2);
                    double playerMul = PlayerHelpers.selectByPlayerId(myId, -1, 1);
                    IInformationSet oppIs = ((CFRDAugmentedCISWrapper)tracker.getCurrentState()).getOpponentsAugmentedIS();
                    double oppCFV = probWithoutOpponent * playerMul * p1Utility;
                    nextOpponentCFV.computeIfPresent(oppIs, (k, oldV) -> oldV + oppCFV);
                    nextReachProbs.computeIfPresent(tracker.getCurrentState(), (k, oldV) -> oldV + probWithoutOpponent/info.stateSamplingProb);
                }
            }
        });
        cummulativeStrategy = cfrSolver.getCumulativeStrat();
        return cfrSolver;
    }

    protected void findMyNextTurn(CFRDTracker tracker, ArrayList<ActionIdxWrapper> actionPath, ObjectTree<ActionIdxWrapper> currentPathTree) {
        ICompleteInformationState s = tracker.getCurrentState();
        visitedStates++;
        if (s.isTerminal()) return;
        if (actionPath != null && s.getActingPlayerId() == myId && s.getInfoSetForActingPlayer().equals(hiddenInfo)) {
            currentPathTree.addPath(actionPath);
        }
        if (tracker.wasMyNextTurnReached()) {
            ICompleteInformationState uf = tracker.getLastSubgameRoot();
            subgameMap.addSubgameState(tracker.getCurrentState(), uf);
            nextOpponentCFV.putIfAbsent(((CFRDAugmentedCISWrapper)uf).getOpponentsAugmentedIS(), 0d);
            nextReachProbs.putIfAbsent(uf, 0d);
            return;
        }
        int actionIdx = 0;
        for (IAction a: s.getLegalActions()) {
            if (actionPath != null) actionPath.add(new ActionIdxWrapper(a, actionIdx));
            findMyNextTurn(tracker.next(a), actionPath, currentPathTree);
            if (actionPath != null) actionPath.remove(actionPath.size() - 1);
            actionIdx++;
        }
    }

    private void findMySubgameTurn(CFRDTracker tracker, int turns) {
        ICompleteInformationState s = tracker.getCurrentState();
        // when next subgame is reached, any state where I act will be re-solved in another subgame
        if (s.isTerminal() || tracker.wasNextSubgameReached()) return;
        if (s.getActingPlayerId() == myId) {
            turns++;
            if (turns == subgameActDepth) {
                subgameActingIs.add(s.getInfoSetForActingPlayer());
                return;
            }
        }
        for (IAction a: s.getLegalActions()) {
            findMySubgameTurn(tracker.next(a), turns);
        }
    }

    protected CFRDTracker prepareDataStructures(ObjectTree<ActionIdxWrapper> currentPathTree) {
        ICompleteInformationState subgame = new CFRDGadgetRoot(range, opponentCFV, opponentCFVNorm, opponentId);
        CFRDTracker tracker = CFRDTracker.create(myId, subgame, range.getNorm());
        findMyNextTurn(tracker, useISTargeting ? new ArrayList<>() : null, currentPathTree);
        return tracker;
    }

    private void runWithPausedTimer(IterationTimer timer, Runnable fn) {
        timer.stop();
        fn.run();
        timer.start();
    }

    private void runSolver(IterationTimer timeout) {
        do {
            timeout.startIteration();
            cfrSolver.runIteration(rootTracker);

            runWithPausedTimer(timeout, () -> resolvingListeners.forEach(listener -> listener.resolvingIterationEnd(resInfo)));
            timeout.endIteration();
            iters++;
        } while (timeout.canDoAnotherIteration());
    }

    @Override
    public ActResult act(IterationTimer timeout, IInformationSet hiddenInfo) {
        this.hiddenInfo = hiddenInfo;
        subgameActDepth++;
        if (cfrSolver != null) {
            // clear visited states count since we're continuing from init
            visitedStates = 0;
            cfrSolver.clearVisitedStates();
        }
        runWithPausedTimer(timeout, () -> resolvingListeners.forEach(listener -> listener.resolvingStart(resInfo)));
        if (cfrSolver == null) {
            createSolver();
            useISTargeting = cfrSolver instanceof ITargetableSolver && ((ITargetableSolver)cfrSolver).wantsTargeting();
            ObjectTree<ActionIdxWrapper> currentPathTree = useISTargeting ? new ObjectTree<>() : null;
            rootTracker = prepareDataStructures(currentPathTree);
            if (useISTargeting) {
                ((ITargetableSolver) cfrSolver).setTargeting(new InfoSetSearchTargeting(currentPathTree));
            }
        }

        runSolver(timeout);
        runWithPausedTimer(timeout, () -> resolvingListeners.forEach(listener -> listener.resolvingEnd(resInfo)));

        return new ActResult(cfrSolver.getCumulativeStrat(), subgameMap, nextReachProbs, nextOpponentCFV, iters, iters);
    }


    @Override
    public void init(ICompleteInformationState initialState, IterationTimer timeout) {
        runWithPausedTimer(timeout, () -> resolvingListeners.forEach(listener -> listener.resolvingStart(resInfo)));
        rootTracker = CFRDTracker.create(myId, initialState, 1);
        findMyNextTurn(rootTracker, null, null);
        createSolver();
        runSolver(timeout);
        runWithPausedTimer(timeout, () -> {
            resolvingListeners.forEach(listener -> listener.resolvingEnd(resInfo));
            resolvingListeners.forEach(listener -> listener.initEnd(resInfo));
        });
    }

    @Override
    public ISubgameResolver copy(ArrayList<IEvaluablePlayer.IListener> listeners) {
        ExternalCFRResolver ret = new ExternalCFRResolver(this);
        ret.resolvingListeners = listeners == null ? new ArrayList<>() : listeners;
        return ret;
    }

    @Override
    public IEvaluablePlayer.IResolvingInfo getResolvingInfo() {
        return resInfo;
    }
}

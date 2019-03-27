package com.ggp.players.continual_resolving.resolvers;

import com.ggp.*;
import com.ggp.player_evaluators.IEvaluablePlayer;
import com.ggp.players.continual_resolving.ISubgameResolver;
import com.ggp.players.continual_resolving.cfrd.AugmentedIS.CFRDAugmentedCISWrapper;
import com.ggp.players.continual_resolving.cfrd.CFRDSubgameRoot;
import com.ggp.players.continual_resolving.cfrd.OpponentsChoiceState;
import com.ggp.players.continual_resolving.cfrd.actions.SelectCISAction;
import com.ggp.players.continual_resolving.trackers.CFRDTracker;
import com.ggp.players.continual_resolving.trackers.IGameTraversalTracker;
import com.ggp.players.continual_resolving.utils.*;
import com.ggp.solvers.cfr.BaseCFRSolver;
import com.ggp.solvers.cfr.ISearchTargeting;
import com.ggp.solvers.cfr.ITargetableSolver;
import com.ggp.utils.PlayerHelpers;
import com.ggp.utils.strategy.NormalizingStrategyWrapper;
import com.ggp.utils.time.IterationTimer;

import java.util.*;

public class ExternalCFRResolver implements ISubgameResolver {
    public static class Factory implements ISubgameResolver.Factory {
        private BaseCFRSolver.Factory solverFactory;

        public Factory(BaseCFRSolver.Factory solverFactory) {
            this.solverFactory = solverFactory;
        }

        @Override
        public ISubgameResolver create(int myId, IInformationSet hiddenInfo, CISRange myRange, HashMap<IInformationSet, Double> opponentCFV,
                                       long opponentCfvNorm, ArrayList<IEvaluablePlayer.IListener> resolvingListeners)
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
    private long opponentCFVNorm;
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

    private class InfoSetTargeting implements ISearchTargeting {
        private final List<Integer> rootTargeting;
        private final List<Integer> opponentsChoiceTargeting;

        public InfoSetTargeting(CFRDSubgameRoot subgame) {
            List<Integer> rootActions = new ArrayList<>();
            int actionIdx = 0;
            for (IAction a: subgame.getLegalActions()) {
                SelectCISAction cisAction = (SelectCISAction) a;
                if (hiddenInfo.equals(cisAction.getSelectedState().getInfoSetForPlayer(myId))) {
                    rootActions.add(actionIdx);
                }
                actionIdx++;
            }
            this.rootTargeting = Collections.unmodifiableList(rootActions);
            // always use the follow action in opponent's choice node
            this.opponentsChoiceTargeting = Collections.singletonList(0);
        }

        @Override
        public List<Integer> target(ICompleteInformationState s) {
            if (s.getClass() == CFRDSubgameRoot.class) return rootTargeting;
            if (s.getClass() == OpponentsChoiceState.class) return opponentsChoiceTargeting;
            return null;
        }

        @Override
        public ISearchTargeting next(IAction a, int actionIdx) {
            return this;
        }
    }

    private class ResolvingInfo implements IEvaluablePlayer.IResolvingInfo {
        @Override
        public IStrategy getNormalizedSubgameStrategy() {
            return new NormalizingStrategyWrapper(cummulativeStrategy);
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
                               long opponentCFVNorm, ArrayList<IEvaluablePlayer.IListener> resolvingListeners,
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
    }

    private BaseCFRSolver createSolver(CFRDSubgameRoot subgame) {
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
        if (subgame != null && cfrSolver instanceof ITargetableSolver) {
            ITargetableSolver s = (ITargetableSolver) cfrSolver;
            //if (s.wantsTargeting()) s.setTargeting(new InfoSetTargeting(subgame));
        }
        cummulativeStrategy = cfrSolver.getCumulativeStrat();
        return cfrSolver;
    }

    protected void findMyNextTurn(CFRDTracker tracker) {
        ICompleteInformationState s = tracker.getCurrentState();
        visitedStates++;
        if (s.isTerminal()) return;
        if (tracker.wasMyNextTurnReached()) {
            ICompleteInformationState uf = tracker.getLastSubgameRoot();
            subgameMap.addSubgameState(tracker.getCurrentState(), uf);
            nextOpponentCFV.putIfAbsent(((CFRDAugmentedCISWrapper)uf).getOpponentsAugmentedIS(), 0d);
            nextReachProbs.putIfAbsent(uf, 0d);
            return;
        }
        for (IAction a: s.getLegalActions()) {
            findMyNextTurn(tracker.next(a));
        }
    }

    protected CFRDTracker prepareDataStructures() {
        ICompleteInformationState subgame = new CFRDSubgameRoot(range, opponentCFV, opponentCFVNorm, opponentId);
        CFRDTracker tracker = CFRDTracker.create(myId, subgame);
        findMyNextTurn(tracker);
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
    public ActResult act(IterationTimer timeout) {
        if (cfrSolver != null) {
            // clear visited states count since we're continuing from init
            visitedStates = 0;
            cfrSolver.clearVisitedStates();
        }
        runWithPausedTimer(timeout, () -> resolvingListeners.forEach(listener -> listener.resolvingStart(resInfo)));
        if (cfrSolver == null) {
            rootTracker = prepareDataStructures();
            createSolver((CFRDSubgameRoot) rootTracker.getCurrentState());
        }

        runSolver(timeout);
        runWithPausedTimer(timeout, () -> resolvingListeners.forEach(listener -> listener.resolvingEnd(resInfo)));

        return new ActResult(cfrSolver.getCumulativeStrat(), subgameMap, nextReachProbs, nextOpponentCFV, iters);
    }


    @Override
    public void init(ICompleteInformationState initialState, IterationTimer timeout) {
        runWithPausedTimer(timeout, () -> resolvingListeners.forEach(listener -> listener.resolvingStart(resInfo)));
        rootTracker = CFRDTracker.create(myId, initialState);
        findMyNextTurn(rootTracker);
        createSolver(null);
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
}

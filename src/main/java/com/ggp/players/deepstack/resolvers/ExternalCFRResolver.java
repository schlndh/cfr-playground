package com.ggp.players.deepstack.resolvers;

import com.ggp.*;
import com.ggp.players.deepstack.IResolvingInfo;
import com.ggp.players.deepstack.IResolvingListener;
import com.ggp.players.deepstack.ISubgameResolver;
import com.ggp.players.deepstack.cfrd.AugmentedIS.CFRDAugmentedCISWrapper;
import com.ggp.players.deepstack.cfrd.CFRDSubgameRoot;
import com.ggp.players.deepstack.cfrd.OpponentsChoiceState;
import com.ggp.players.deepstack.cfrd.actions.SelectCISAction;
import com.ggp.players.deepstack.trackers.CFRDTracker;
import com.ggp.players.deepstack.trackers.IGameTraversalTracker;
import com.ggp.players.deepstack.utils.*;
import com.ggp.solvers.cfr.BaseCFRSolver;
import com.ggp.solvers.cfr.ISearchTargeting;
import com.ggp.solvers.cfr.ITargetableSolver;
import com.ggp.utils.PlayerHelpers;
import com.ggp.utils.strategy.Strategy;

import java.util.*;

public class ExternalCFRResolver implements ISubgameResolver {
    public static class Factory implements ISubgameResolver.Factory {
        private BaseCFRSolver.Factory solverFactory;
        private final boolean useISTargeting;

        public Factory(BaseCFRSolver.Factory solverFactory, boolean useISTargeting) {
            this.solverFactory = solverFactory;
            this.useISTargeting = useISTargeting;
        }

        @Override
        public ISubgameResolver create(int myId, IInformationSet hiddenInfo, CISRange myRange, HashMap<IInformationSet, Double> opponentCFV,
                                       ArrayList<IResolvingListener> resolvingListeners)
        {
            return new ExternalCFRResolver(myId, hiddenInfo, myRange, opponentCFV, resolvingListeners, solverFactory, useISTargeting);
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
    private List<IResolvingListener> resolvingListeners;
    private IResolvingInfo resInfo = new ResolvingInfo();
    private final int opponentId;

    private BaseCFRSolver.Factory solverFactory;
    private SubgameMap subgameMap;
    private NextRangeTree nrt = new NextRangeTree();
    private HashMap<IInformationSet, Double> nextOpponentCFV = new HashMap<>();
    private IStrategy cummulativeStrategy;
    private BaseCFRSolver lastSolver = null;
    private final boolean useISTargeting;

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
        public ISearchTargeting next(IAction a) {
            return this;
        }
    }

    private class ResolvingInfo implements IResolvingInfo {
        @Override
        public IStrategy getUnnormalizedCumulativeStrategy() {
            return cummulativeStrategy;
        }

        @Override
        public IInformationSet getHiddenInfo() {
            return hiddenInfo;
        }

        @Override
        public long getVisitedStatesInCurrentResolving() {
            if (lastSolver == null) return 0;
            return lastSolver.getVisitedStates();
        }
    }

    public ExternalCFRResolver(int myId, IInformationSet hiddenInfo, CISRange range, HashMap<IInformationSet, Double> opponentCFV,
                               ArrayList<IResolvingListener> resolvingListeners, BaseCFRSolver.Factory solverFactory, boolean useISTargeting)
    {
        this.myId = myId;
        this.hiddenInfo = hiddenInfo;
        this.range = range;
        this.opponentCFV = opponentCFV;
        this.resolvingListeners = resolvingListeners;
        if (this.resolvingListeners == null) this.resolvingListeners = new ArrayList<>();
        this.opponentId = PlayerHelpers.getOpponentId(myId);
        this.solverFactory = solverFactory;
        this.useISTargeting = useISTargeting;
        this.subgameMap = new SubgameMap(opponentId);
    }

    private BaseCFRSolver createSolver(CFRDSubgameRoot subgame, HashSet<IInformationSet> accumulatedInfoSets) {
        BaseCFRSolver cfrSolver = solverFactory.create(new BaseCFRSolver.IStrategyAccumulationFilter() {
            @Override
            public boolean isAccumulated(IInformationSet is) {
                return accumulatedInfoSets != null && accumulatedInfoSets.contains(is);
            }

            @Override
            public Iterable<IInformationSet> getAccumulated() {
                if (accumulatedInfoSets == null) return Collections.EMPTY_LIST;
                return accumulatedInfoSets;
            }
        });

        cfrSolver.registerListener(new BaseCFRSolver.IListener() {
            @Override
            public void enteringState(IGameTraversalTracker tracker, BaseCFRSolver.Info info) {
            }

            @Override
            public void leavingState(IGameTraversalTracker t, BaseCFRSolver.Info info, double p1Utility) {
                CFRDTracker tracker = (CFRDTracker) t;
                if (tracker.isMyNextTurnReached()) {
                    double probWithoutOpponent = info.rndProb * PlayerHelpers.selectByPlayerId(myId, info.reachProb1, info.reachProb2);
                    double playerMul = PlayerHelpers.selectByPlayerId(myId, -1, 1);
                    IInformationSet oppIs = ((CFRDAugmentedCISWrapper)tracker.getCurrentState()).getOpponentsAugmentedIS();
                    double oppCFV = probWithoutOpponent * playerMul * p1Utility;
                    nextOpponentCFV.merge(oppIs, oppCFV, (oldV, newV) -> oldV + newV);
                }
            }
        });
        if (useISTargeting && subgame != null && cfrSolver instanceof ITargetableSolver) {
            ITargetableSolver s = (ITargetableSolver) cfrSolver;
            s.setTargeting(new InfoSetTargeting(subgame));
        }
        cummulativeStrategy = cfrSolver.getCumulativeStrat();
        lastSolver = cfrSolver;
        return cfrSolver;
    }

    protected void findMyNextTurn(CFRDTracker tracker) {
        ICompleteInformationState s = tracker.getCurrentState();
        if (s.isTerminal()) return;
        if (tracker.isMyNextTurnReached()) {
            subgameMap.addSubgameState(s);
            nrt.add(s, tracker.getMyFirstIS(), tracker.getMyTopAction(), tracker.getRndProb());
            nextOpponentCFV.putIfAbsent(((CFRDAugmentedCISWrapper)tracker.getCurrentState()).getOpponentsAugmentedIS(), 0d);
            return;
        }
        for (IAction a: s.getLegalActions()) {
            findMyNextTurn(tracker.next(a));
        }
    }

    protected CFRDTracker prepareDataStructures() {
        ICompleteInformationState subgame = new CFRDSubgameRoot(range, opponentCFV, opponentId);
        CFRDTracker tracker = CFRDTracker.createForAct(myId, range.getNorm(), subgame);
        findMyNextTurn(tracker);
        return tracker;
    }

    private void runWithPausedTimer(IterationTimer timer, Runnable fn) {
        timer.stop();
        fn.run();
        timer.start();
    }

    @Override
    public ActResult act(IterationTimer timeout) {
        runWithPausedTimer(timeout, () -> resolvingListeners.forEach(listener -> listener.resolvingStart(resInfo)));
        CFRDTracker tracker = prepareDataStructures();

        HashSet<IInformationSet> myInformationSets = new HashSet<>();
        for (ICompleteInformationState s: range.getPossibleStates()) {
            myInformationSets.add(s.getInfoSetForActingPlayer());
        }
        int iters = 0;
        BaseCFRSolver cfrSolver = createSolver((CFRDSubgameRoot) tracker.getCurrentState(), myInformationSets);
        while (timeout.canDoAnotherIteration()) {
            timeout.startIteration();
            cfrSolver.runIteration(tracker);

            runWithPausedTimer(timeout, () -> resolvingListeners.forEach(listener -> listener.resolvingIterationEnd(resInfo)));
            timeout.endIteration();
            iters++;
        }

        runWithPausedTimer(timeout, () -> resolvingListeners.forEach(listener -> listener.resolvingEnd(resInfo)));

        Strategy cumulativeStrat = cfrSolver.getFinalCumulativeStrat();
        cumulativeStrat.normalize();
        this.cummulativeStrategy = cumulativeStrat;
        int cfvNorm = iters;
        nextOpponentCFV.replaceAll((is, cfv) -> cfv/cfvNorm);
        lastSolver = null;

        return new ActResult(cumulativeStrat, subgameMap, nrt, nextOpponentCFV);
    }


    @Override
    public InitResult init(ICompleteInformationState initialState, IterationTimer timeout) {
        runWithPausedTimer(timeout, () -> resolvingListeners.forEach(listener -> listener.resolvingStart(resInfo)));
        CFRDTracker tracker = CFRDTracker.createForInit(myId, initialState);
        findMyNextTurn(tracker);
        InitResult res = doInit(tracker, timeout);
        runWithPausedTimer(timeout, () -> {
            resolvingListeners.forEach(listener -> listener.resolvingEnd(resInfo));
            resolvingListeners.forEach(listener -> listener.initEnd(resInfo));
        });

        lastSolver = null;
        return res;
    }

    protected InitResult doInit(CFRDTracker tracker, IterationTimer timeout) {
        int iters = 0;
        BaseCFRSolver cfrSolver = createSolver(null, null);
        while (timeout.canDoAnotherIteration()) {
            timeout.startIteration();
            cfrSolver.runIteration(tracker);
            runWithPausedTimer(timeout, () -> resolvingListeners.forEach(listener -> listener.resolvingIterationEnd(resInfo)));
            timeout.endIteration();
            iters++;
        }
        int cfvNorm = iters;
        nextOpponentCFV.replaceAll((is, cfv) -> cfv/cfvNorm);
        return new InitResult(subgameMap, nrt, nextOpponentCFV);
    }
}

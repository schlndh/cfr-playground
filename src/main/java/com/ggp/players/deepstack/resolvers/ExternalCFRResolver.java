package com.ggp.players.deepstack.resolvers;

import com.ggp.*;
import com.ggp.players.deepstack.IResolvingInfo;
import com.ggp.players.deepstack.IResolvingListener;
import com.ggp.players.deepstack.ISubgameResolver;
import com.ggp.players.deepstack.cfrd.CFRDSubgameRoot;
import com.ggp.players.deepstack.trackers.CFRDTracker;
import com.ggp.players.deepstack.trackers.IGameTraversalTracker;
import com.ggp.players.deepstack.utils.InformationSetRange;
import com.ggp.players.deepstack.utils.IterationTimer;
import com.ggp.players.deepstack.utils.Strategy;
import com.ggp.solvers.cfr.BaseCFRSolver;
import com.ggp.utils.PlayerHelpers;

import java.util.*;

public class ExternalCFRResolver implements ISubgameResolver {
    public static class Factory implements ISubgameResolver.Factory {
        private BaseCFRSolver.Factory solverFactory;

        public Factory(BaseCFRSolver.Factory solverFactory) {
            this.solverFactory = solverFactory;
        }

        @Override
        public ISubgameResolver create(int myId, IInformationSet hiddenInfo, InformationSetRange myRange, HashMap<IInformationSet, Double> opponentCFV,
                                       ICompleteInformationStateFactory cisFactory, ArrayList<IResolvingListener> resolvingListeners)
        {
            return new ExternalCFRResolver(myId, hiddenInfo, myRange, opponentCFV, cisFactory, resolvingListeners, solverFactory);
        }

        @Override
        public String getConfigString() {
            return solverFactory.getConfigString();
        }
    }

    private final int myId;
    private IInformationSet hiddenInfo;
    private InformationSetRange range;
    private HashMap<IInformationSet, Double> opponentCFV;
    private List<IResolvingListener> resolvingListeners;
    private IResolvingInfo resInfo = new ResolvingInfo();
    private final int opponentId;

    private BaseCFRSolver.Factory solverFactory;
    private Strategy cumulativeStrat;

    private class ResolvingInfo implements IResolvingInfo {
        @Override
        public Strategy getUnnormalizedCumulativeStrategy() {
            return cumulativeStrat;
        }

        @Override
        public IInformationSet getHiddenInfo() {
            return hiddenInfo;
        }
    }

    public ExternalCFRResolver(int myId, IInformationSet hiddenInfo, InformationSetRange range, HashMap<IInformationSet, Double> opponentCFV,
                       ICompleteInformationStateFactory cisFactory, ArrayList<IResolvingListener> resolvingListeners, BaseCFRSolver.Factory solverFactory)
    {
        this.myId = myId;
        this.hiddenInfo = hiddenInfo;
        this.range = range;
        this.opponentCFV = opponentCFV;
        this.resolvingListeners = resolvingListeners;
        if (this.resolvingListeners == null) this.resolvingListeners = new ArrayList<>();
        this.opponentId = PlayerHelpers.getOpponentId(myId);
        this.solverFactory = solverFactory;
    }

    private BaseCFRSolver createSolver(HashSet<IInformationSet> accumulatedInfoSets) {
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
        cumulativeStrat = cfrSolver.getCumulativeStrat();

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
                    tracker.getNtit().addLeaf(tracker.getCurrentState().getInfoSetForPlayer(opponentId), probWithoutOpponent * playerMul * p1Utility);
                }
            }
        });
        return cfrSolver;
    }

    protected void findMyNextTurn(CFRDTracker tracker) {
        ICompleteInformationState s = tracker.getCurrentState();
        if (s.isTerminal()) return;
        if (tracker.isMyNextTurnReached()) {
            tracker.getNtit().addLeaf(s.getInfoSetForPlayer(opponentId), 0);
            tracker.getPsMap().add(tracker.getMyPerceptSequence(), tracker.getOpponentPerceptSequence());
            tracker.getNrt().add(tracker.getOpponentPerceptSequence(), s, tracker.getMyTopAction(), tracker.getRndProb());
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

    @Override
    public ActResult act(IterationTimer timeout) {
        resolvingListeners.forEach(listener -> listener.resolvingStart(resInfo));
        CFRDTracker tracker = prepareDataStructures();
        ActResult res = doAct(tracker, timeout);
        resolvingListeners.forEach(listener -> listener.resolvingEnd(resInfo));
        return res;
    }


    @Override
    public InitResult init(ICompleteInformationState initialState, IterationTimer timeout) {
        resolvingListeners.forEach(listener -> listener.resolvingStart(resInfo));
        CFRDTracker tracker = CFRDTracker.createForInit(myId, initialState);
        findMyNextTurn(tracker);
        InitResult res = doInit(tracker, timeout);
        resolvingListeners.forEach(listener -> listener.resolvingEnd(resInfo));
        resolvingListeners.forEach(listener -> listener.initEnd(resInfo));
        return res;
    }

    protected ActResult doAct(CFRDTracker subgameTracker, IterationTimer timeout) {
        HashSet<IInformationSet> myInformationSets = new HashSet<>();
        for (ICompleteInformationState s: range.getPossibleStates()) {
            myInformationSets.add(s.getInfoSetForActingPlayer());
        }
        int iters = 0;
        BaseCFRSolver cfrSolver = createSolver(myInformationSets);
        while (timeout.canDoAnotherIteration()) {
            timeout.startIteration();
            cfrSolver.runIteration(subgameTracker);

            resolvingListeners.forEach(listener -> listener.resolvingIterationEnd(resInfo));
            timeout.endIteration();
            iters++;
        }
        cumulativeStrat.normalize();

        return new ActResult(cumulativeStrat, subgameTracker.getActionToNtit(), subgameTracker.getActionToPsMap(), subgameTracker.getMyISToNRT(), iters);
    }

    protected InitResult doInit(CFRDTracker tracker, IterationTimer timeout) {
        int iters = 0;
        BaseCFRSolver cfrSolver = createSolver(null);
        while (timeout.canDoAnotherIteration()) {
            timeout.startIteration();
            cfrSolver.runIteration(tracker);
            resolvingListeners.forEach(listener -> listener.resolvingIterationEnd(resInfo));
            timeout.endIteration();
            iters++;
        }
        return new InitResult(tracker.getNtit(), tracker.getNrt(), tracker.getPsMap(), iters);
    }
}

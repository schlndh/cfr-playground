package com.ggp.solvers.cfr;

import com.ggp.*;
import com.ggp.solvers.cfr.is_info.BaseCFRISInfo;
import com.ggp.utils.IUtilityEstimator;
import com.ggp.players.continual_resolving.trackers.IGameTraversalTracker;
import com.ggp.utils.PlayerHelpers;

import java.util.List;

public class DepthLimitedCFRSolver extends BaseCFRSolver {
    public static class Factory extends BaseCFRSolver.Factory {
        private int depthLimit;
        private boolean alternatingUpdates;
        private IUtilityEstimator.IFactory ueFactory;
        private final double cumulativeStratExp;

        public Factory(IRegretMatching.IFactory rmFactory, int depthLimit, IUtilityEstimator.IFactory ueFactory,
                       boolean alternatingUpdates, double cumulativeStratExp) {
            super(rmFactory);
            this.depthLimit = depthLimit;
            this.ueFactory = ueFactory;
            this.alternatingUpdates = alternatingUpdates;
            this.cumulativeStratExp = cumulativeStratExp;
        }

        @Override
        public BaseCFRSolver create(IStrategyAccumulationFilter accumulationFilter) {
            return new DepthLimitedCFRSolver(rmFactory, accumulationFilter, depthLimit,
                    (ueFactory == null ? null : ueFactory.create()), alternatingUpdates, cumulativeStratExp);
        }

        @Override
        public String getConfigString() {
            return "CFR{" +
                    "ue=" + ((ueFactory == null) ? "null" : ueFactory.getConfigString()) +
                    ",dl=" + depthLimit +
                    ",rm=" + rmFactory.getConfigString() +
                    ",au=" + alternatingUpdates +
                    ",cse=" + cumulativeStratExp +
                    '}';
        }
    }

    private int depthLimit = 0;
    private IUtilityEstimator utilityEstimator;
    private long iterationCounter = 0;
    private final boolean alternatingUpdates;
    private final double cumulativeStratExp;
    private boolean[] updatePlayer = new boolean[]{false, true, true};

    protected DepthLimitedCFRSolver(DepthLimitedCFRSolver solver, IStrategyAccumulationFilter accumulationFilter) {
        super(solver, accumulationFilter);
        this.depthLimit = solver.depthLimit;
        this.utilityEstimator = solver.utilityEstimator;
        if (this.utilityEstimator != null) this.utilityEstimator = this.utilityEstimator.copy();
        this.iterationCounter = solver.iterationCounter;
        this.alternatingUpdates = solver.alternatingUpdates;
        this.cumulativeStratExp = solver.cumulativeStratExp;
    }

    public DepthLimitedCFRSolver(IRegretMatching.IFactory rmFactory, IStrategyAccumulationFilter accumulationFilter,
                                 int depthLimit, IUtilityEstimator utilityEstimator, boolean alternatingUpdates, double cumulativeStratExp) {
        super(rmFactory, accumulationFilter);
        this.depthLimit = depthLimit;
        this.utilityEstimator = utilityEstimator;
        this.alternatingUpdates = alternatingUpdates;
        this.cumulativeStratExp = cumulativeStratExp;
    }

    @Override
    public BaseCFRSolver copy(IStrategyAccumulationFilter accumulationFilter) {
        return new DepthLimitedCFRSolver(this, accumulationFilter);
    }

    /**
     * Run CFR
     * @param tracker
     * @param depth
     * @param reachProb1
     * @param reachProb2
     * @return 1st player utility of root state under current strategy
     */
    private double cfr(IGameTraversalTracker tracker, int depth, double reachProb1, double reachProb2) {
        // CVF_i(h) = reachProb_{-i}(h) * utility_i(H)
        // this method passes reachProb from top and returns player 1's utility
        ICompleteInformationState s = tracker.getCurrentState();
        Info info = new Info(reachProb1, reachProb2, tracker.getRndProb());
        visitedStates++;
        listeners.forEach(listener -> listener.enteringState(tracker, info));

        if (s.isTerminal()) {
            return s.getPayoff(1);
        }

        if (depth > depthLimit && utilityEstimator != null && utilityEstimator.canEstimate(tracker)) {
            IUtilityEstimator.UtilityEstimate res = utilityEstimator.estimate(tracker);
            visitedStates += res.visitedStates;
            return res.p1Utility;
        }
        List<IAction> legalActions = s.getLegalActions();
        double rndProb = tracker.getRndProb();

        if (s.isRandomNode()) {
            IRandomNode rndNode = s.getRandomNode();
            double ret = 0;
            for (IRandomNode.IRandomNodeAction rndAction: rndNode) {
                double actionProb = rndAction.getProb();
                ret += actionProb * cfr(tracker.next(rndAction.getAction()), depth+1, reachProb1, reachProb2);
            }
            return ret;
        }

        IInformationSet is = s.getInfoSetForActingPlayer();
        BaseCFRISInfo isInfo = getIsInfo(is);
        double utility = 0;
        double[] actionUtility = new double[legalActions.size()];

        int actionIdx = 0;
        for (IAction a: legalActions) {
            double actionProb = isInfo.getStrat()[actionIdx];
            double np1 = reachProb1, np2 = reachProb2;
            if (s.getActingPlayerId() == 1) {
                np1 *= actionProb;
            } else if (s.getActingPlayerId() == 2) {
                np2 *= actionProb;
            }
            actionUtility[actionIdx] = cfr(tracker.next(a), depth+1, np1, np2);
            utility = utility + actionProb*actionUtility[actionIdx];
            actionIdx++;
        }
        final double finUtility =  utility;
        listeners.forEach(listener -> listener.leavingState(tracker, info, finUtility));

        actionIdx = 0;
        double probWithoutActingPlayer = rndProb * PlayerHelpers.selectByPlayerId(s.getActingPlayerId(), reachProb2, reachProb1); // reachProb_{-i}
        int pid = s.getActingPlayerId();
        if (updatePlayer[pid]) {
            for (IAction a: legalActions) {
                double playerMul = PlayerHelpers.selectByPlayerId(pid, 1, -1);
                addRegret(isInfo, actionIdx, probWithoutActingPlayer * playerMul * (actionUtility[actionIdx] - utility));
                actionIdx++;
            }

            if (accumulationFilter.isAccumulated(is)) {
                double playerReachProb = rndProb * PlayerHelpers.selectByPlayerId(s.getActingPlayerId(), reachProb1, reachProb2);
                double[] strat = isInfo.getStrat();
                double[] cumulativeStrat = isInfo.getCumulativeStrat();
                double mul = Math.pow(((double) iterationCounter) / (iterationCounter + 1), cumulativeStratExp);
                for (int a = 0; a < strat.length; ++a) {
                    cumulativeStrat[a] = mul * cumulativeStrat[a] + playerReachProb * strat[a];
                }
            }
        }


        return utility;
    }

    @Override
    public void runIteration(IGameTraversalTracker tracker) {
        iterationCounter++;
        int player = (int)(iterationCounter % 2) + 1;
        if (alternatingUpdates) {
            updatePlayer[player] = true;
            updatePlayer[PlayerHelpers.getOpponentId(player)] = false;
        }

        cfr(tracker, 0, 1, 1);
        isInfos.forEach((is, isInfo) -> {if (updatePlayer[is.getOwnerId()]) isInfo.doRegretMatching();});
    }
}

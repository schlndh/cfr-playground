package com.ggp.solvers.cfr;

import com.ggp.*;
import com.ggp.players.deepstack.IRegretMatching;
import com.ggp.IInfoSetStrategy;
import com.ggp.utils.IUtilityEstimator;
import com.ggp.players.deepstack.trackers.IGameTraversalTracker;
import com.ggp.utils.PlayerHelpers;

import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;

public class DepthLimitedCFRSolver extends BaseCFRSolver {
    public static class Factory extends BaseCFRSolver.Factory {
        private int depthLimit = 0;
        private IUtilityEstimator.IFactory ueFactory;

        public Factory(IRegretMatching.Factory rmFactory) {
            super(rmFactory);
        }

        public Factory(IRegretMatching.Factory rmFactory, int depthLimit, IUtilityEstimator.IFactory ueFactory) {
            super(rmFactory);
            this.depthLimit = depthLimit;
            this.ueFactory = ueFactory;
        }

        @Override
        public BaseCFRSolver create(IStrategyAccumulationFilter accumulationFilter) {
            return new DepthLimitedCFRSolver(rmFactory.create(), accumulationFilter, depthLimit, (ueFactory == null ? null : ueFactory.create()));
        }

        @Override
        public String getConfigString() {
            return "CFR{" +
                    "ue=" + ((ueFactory == null) ? "null" : ueFactory.getConfigString()) +
                    ", dl=" + depthLimit +
                    ", rm=" + rmFactory.getConfigString() +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Factory factory = (Factory) o;
            return depthLimit == factory.depthLimit &&
                    Objects.equals(ueFactory, factory.ueFactory);
        }

        @Override
        public int hashCode() {
            return Objects.hash(depthLimit, ueFactory);
        }
    }

    private int depthLimit = 0;
    private IUtilityEstimator utilityEstimator;

    public DepthLimitedCFRSolver(IRegretMatching regretMatching, IStrategyAccumulationFilter accumulationFilter,
                                 int depthLimit, IUtilityEstimator utilityEstimator) {
        super(regretMatching, accumulationFilter);
        this.depthLimit = depthLimit;
        this.utilityEstimator = utilityEstimator;
    }

    /**
     * Run CFR
     * @param tracker
     * @param player
     * @param depth
     * @param reachProb1
     * @param reachProb2
     * @return 1st player utility of root state under current strategy
     */
    private double cfr(IGameTraversalTracker tracker, int player, int depth, double reachProb1, double reachProb2) {
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

        BiFunction<ICompleteInformationState, Integer, Double> callCfr = (x, actionIdx) -> {
            double np1 = reachProb1, np2 = reachProb2;
            IInfoSetStrategy isStrat = strat.getInfoSetStrategy(s.getInfoSetForActingPlayer());
            if (s.getActingPlayerId() == 1) {
                np1 *= isStrat.getProbability(actionIdx);
            } else if (s.getActingPlayerId() == 2) {
                np2 *= isStrat.getProbability(actionIdx);
            }
            IAction a = legalActions.get(actionIdx);
            return cfr(tracker.next(a), player, depth+1, np1, np2);
        };

        if (s.isRandomNode()) {
            IRandomNode rndNode = s.getRandomNode();
            double ret = 0;
            int actionIdx = 0;
            for (IRandomNode.IRandomNodeAction rndAction: rndNode) {
                double actionProb = rndAction.getProb();
                ret += actionProb * callCfr.apply(s, actionIdx++);
            }
            return ret;
        }

        IInformationSet is = s.getInfoSetForActingPlayer();
        IInfoSetStrategy isStrat = strat.getInfoSetStrategy(is);
        double utility = 0;
        double[] actionUtility = new double[legalActions.size()];


        int actionIdx = 0;
        for (IAction a: legalActions) {
            double actionProb = isStrat.getProbability(actionIdx);
            double res = callCfr.apply(s, actionIdx);
            actionUtility[actionIdx] = res;
            utility = utility + actionProb*actionUtility[actionIdx];
            actionIdx++;
        }
        final double finUtility =  utility;
        listeners.forEach(listener -> listener.leavingState(tracker, info, finUtility));

        actionIdx = 0;
        double probWithoutActingPlayer = rndProb * PlayerHelpers.selectByPlayerId(s.getActingPlayerId(), reachProb2, reachProb1); // reachProb_{-i}
        int pid = s.getActingPlayerId();
        for (IAction a: legalActions) {
            double playerMul = PlayerHelpers.selectByPlayerId(pid, 1, -1);
            regretMatching.addActionRegret(is, actionIdx, probWithoutActingPlayer * playerMul * (actionUtility[actionIdx] - utility));
            actionIdx++;
        }

        return utility;
    }

    @Override
    public void runIteration(IGameTraversalTracker tracker) {
        cfr(tracker, 1, 0, 1, 1);
        for (IInformationSet is: accumulationFilter.getAccumulated()) {
            IInfoSetStrategy isStrat = strat.getInfoSetStrategy(is);
            cumulativeStrat.addProbabilities(is, (actionIdx) -> isStrat.getProbability(actionIdx));
        }
        regretMatching.getRegretMatchedStrategy(strat);
    }
}

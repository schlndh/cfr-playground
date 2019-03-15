package com.ggp.utils;

import com.ggp.players.continual_resolving.trackers.IGameTraversalTracker;

public interface IUtilityEstimator {
    interface IFactory {
        IUtilityEstimator create();
        String getConfigString();
    }

    class UtilityEstimate {
        public double p1Utility;
        public long visitedStates;

        public UtilityEstimate(double p1Utility, long visitedStates) {
            this.p1Utility = p1Utility;
            this.visitedStates = visitedStates;
        }
    }

    /**
     * Estimate state's utility
     * @param tracker
     * @return 1st player utility
     */
    UtilityEstimate estimate(IGameTraversalTracker tracker);

    /**
     * Checks wheter estimator can estimate utility of given state
     * @param tracker
     * @return
     */
    boolean canEstimate(IGameTraversalTracker tracker);

    IUtilityEstimator copy();
}

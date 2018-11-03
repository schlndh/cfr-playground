package com.ggp.utils;

import com.ggp.players.deepstack.trackers.IGameTraversalTracker;

public interface IUtilityEstimator {
    interface IFactory {
        IUtilityEstimator create();
        String getConfigString();
    }

    /**
     * Estimate state's utility
     * @param tracker
     * @return 1st player utility
     */
    double estimate(IGameTraversalTracker tracker);

    /**
     * Checks wheter estimator can estimate utility of given state
     * @param tracker
     * @return
     */
    boolean canEstimate(IGameTraversalTracker tracker);
}

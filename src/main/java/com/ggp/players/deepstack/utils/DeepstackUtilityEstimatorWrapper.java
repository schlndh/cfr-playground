package com.ggp.players.deepstack.utils;

import com.ggp.players.deepstack.trackers.CFRDTracker;
import com.ggp.players.deepstack.trackers.IGameTraversalTracker;
import com.ggp.utils.IUtilityEstimator;

public class DeepstackUtilityEstimatorWrapper implements IUtilityEstimator {
    public static class Factory implements  IUtilityEstimator.IFactory {
        private IUtilityEstimator.IFactory ueFactory;

        public Factory(IFactory ueFactory) {
            this.ueFactory = ueFactory;
        }

        @Override
        public IUtilityEstimator create() {
            return null;
        }

        @Override
        public String getConfigString() {
            return ueFactory.getConfigString();
        }
    }

    private IUtilityEstimator utilityEstimator;

    public DeepstackUtilityEstimatorWrapper(IUtilityEstimator utilityEstimator) {
        this.utilityEstimator = utilityEstimator;
    }

    @Override
    public UtilityEstimate estimate(IGameTraversalTracker tracker) {
        return utilityEstimator.estimate(tracker);
    }

    @Override
    public boolean canEstimate(IGameTraversalTracker tracker) {
        return ((CFRDTracker)tracker).wasMyNextTurnReached() && utilityEstimator.canEstimate(tracker);
    }
}

package com.ggp.utils.estimators;

import com.ggp.IAction;
import com.ggp.ICompleteInformationState;
import com.ggp.IRandomNode;
import com.ggp.players.deepstack.trackers.IGameTraversalTracker;
import com.ggp.utils.IUtilityEstimator;
import com.ggp.utils.random.RandomSampler;

import java.util.List;
import java.util.Objects;

public class RandomPlayoutUtilityEstimator implements IUtilityEstimator {
    public static class Factory implements IUtilityEstimator.IFactory {
        private final int iters;

        public Factory() {
            this(1);
        }

        public Factory(int iters) {
            this.iters = iters;
        }

        @Override
        public IUtilityEstimator create() {
            return new RandomPlayoutUtilityEstimator(iters);
        }

        @Override
        public String getConfigString() {
            return "RandomPlayout{" +
                        iters +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Factory factory = (Factory) o;
            return iters == factory.iters;
        }

        @Override
        public int hashCode() {
            return Objects.hash(iters);
        }
    };

    private final int iters;
    private RandomSampler rnd = new RandomSampler();

    public RandomPlayoutUtilityEstimator(int iters) {
        this.iters = iters;
    }

    @Override
    public UtilityEstimate estimate(IGameTraversalTracker tracker) {
        ICompleteInformationState s = tracker.getCurrentState();
        if (s.isTerminal()) {
            return new UtilityEstimate(s.getPayoff(1), 1);
        }
        double u1 = 0;
        double totalProb = 0;
        long visitedStates = 0;
        for (int i = 0; i < iters; ++i) {
            ICompleteInformationState ws = s;
            double prob = 1;
            while (!ws.isTerminal()) {
                visitedStates++;
                List<IAction> legalActions = ws.getLegalActions();
                IAction a;
                if (ws.isRandomNode()) {
                    IRandomNode rndNode = ws.getRandomNode();
                    a = rnd.select(legalActions, action -> rndNode.getActionProb(action)).getResult();
                    prob *= rndNode.getActionProb(a);
                } else {
                    prob *= 1d/legalActions.size();
                    a = rnd.select(legalActions);
                }

                ws = ws.next(a);
            }
            u1 += prob*ws.getPayoff(1);
            totalProb += prob;
        }
        return new UtilityEstimate(u1/totalProb, visitedStates);
    }

    @Override
    public boolean canEstimate(IGameTraversalTracker tracker) {
        return true;
    }

    @Override
    public IUtilityEstimator copy() {
        return new RandomPlayoutUtilityEstimator(iters);
    }
}

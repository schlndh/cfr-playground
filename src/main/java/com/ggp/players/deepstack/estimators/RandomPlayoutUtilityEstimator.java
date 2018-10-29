package com.ggp.players.deepstack.estimators;

import com.ggp.IAction;
import com.ggp.ICompleteInformationState;
import com.ggp.IRandomNode;
import com.ggp.players.deepstack.IUtilityEstimator;
import com.ggp.utils.random.RandomSampler;

import java.util.List;

public class RandomPlayoutUtilityEstimator implements IUtilityEstimator {
    public static class Factory implements IUtilityEstimator.IFactory {
        private final int iters = 5;
        @Override
        public IUtilityEstimator create() {
            return new RandomPlayoutUtilityEstimator(iters);
        }

        @Override
        public String getConfigString() {
            return "rand{" +
                    "i=" + iters +
                    '}';
        }
    };

    private final int iters;
    private RandomSampler rnd = new RandomSampler();

    public RandomPlayoutUtilityEstimator(int iters) {
        this.iters = iters;
    }

    @Override
    public EstimatorResult estimate(ICompleteInformationState s) {
        if (s.isTerminal()) {
            return new EstimatorResult(s.getPayoff(1), s.getPayoff(2));
        }
        double u1 = 0, u2 = 0;
        double totalProb = 0;
        for (int i = 0; i < iters; ++i) {
            ICompleteInformationState ws = s;
            double prob = 1;
            while (!ws.isTerminal()) {
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
            u2 += prob*ws.getPayoff(2);
            totalProb += prob;
        }
        return new EstimatorResult(u1/totalProb, u2/totalProb);
    }
}

package com.ggp.solvers.cfr.baselines;

import com.ggp.IAction;
import com.ggp.IInformationSet;
import com.ggp.solvers.cfr.IBaseline;

import java.util.HashMap;

public class ExponentiallyDecayingAverageBaseline implements IBaseline {
    public static class Factory implements IFactory {
        private final double alpha;

        public Factory(double alpha) {
            this.alpha = alpha;
        }

        @Override
        public IBaseline create() {
            return new ExponentiallyDecayingAverageBaseline(alpha);
        }

        @Override
        public String toString() {
            return "ExpAvg{" +
                        alpha +
                    '}';
        }

        @Override
        public String getConfigString() {
            return toString();
        }
    }
    private final double alpha;
    private final HashMap<IInformationSet, HashMap<IAction, Double>> baselineValues = new HashMap<>();

    public ExponentiallyDecayingAverageBaseline(double alpha) {
        this.alpha = alpha;
    }

    @Override
    public double getValue(IInformationSet is, IAction a) {
        return baselineValues.computeIfAbsent(is, k -> new HashMap<>()).computeIfAbsent(a, k-> 0d);
    }

    @Override
    public void update(IInformationSet is, IAction a, double utilityEstimate) {
        baselineValues.computeIfAbsent(is, k -> new HashMap<>()).merge(a, utilityEstimate,
                (oldV, newV) -> (1-alpha)*(oldV == null ? 0 : oldV) + alpha * newV);
    }
}

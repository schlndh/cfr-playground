package com.ggp.solvers.cfr.baselines;

import com.ggp.solvers.cfr.IBaseline;

import java.util.Arrays;

public class ExponentiallyDecayingAverageBaseline implements IBaseline {
    public static class Factory implements IFactory {
        private final double alpha;

        public Factory(double alpha) {
            this.alpha = alpha;
        }

        @Override
        public IBaseline create(int actionSize) {
            return new ExponentiallyDecayingAverageBaseline(alpha, actionSize);
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
    private double[] baselineValues;

    private ExponentiallyDecayingAverageBaseline(ExponentiallyDecayingAverageBaseline bl) {
        this.alpha = bl.alpha;
        this.baselineValues = Arrays.copyOf(bl.baselineValues, bl.baselineValues.length);
    }

    public ExponentiallyDecayingAverageBaseline(double alpha, int actionSize) {
        this.alpha = alpha;
        this.baselineValues = new double[actionSize];
    }

    @Override
    public double getValue(int actionIdx) {
        if (actionIdx < 0 || actionIdx >= baselineValues.length) return 0;
        return baselineValues[actionIdx];
    }

    @Override
    public void update(int actionIdx, double utilityEstimate) {
        baselineValues[actionIdx] = (1-alpha)*baselineValues[actionIdx] + alpha * utilityEstimate;
    }

    @Override
    public IBaseline copy() {
        return new ExponentiallyDecayingAverageBaseline(this);
    }
}

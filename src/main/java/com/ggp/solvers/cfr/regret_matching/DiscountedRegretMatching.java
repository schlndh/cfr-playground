package com.ggp.solvers.cfr.regret_matching;

import com.ggp.solvers.cfr.IRegretMatching;

import java.util.Arrays;

public class DiscountedRegretMatching extends BaseRegretMatching {
    public static class Factory implements IRegretMatching.IFactory {
        private final double posExp;
        private final double negExp;

        public Factory(double posExp, double negExp) {
            this.posExp = posExp;
            this.negExp = negExp;
        }

        @Override
        public IRegretMatching create(int actionSize) {
            return new DiscountedRegretMatching(actionSize, posExp, negExp);
        }

        @Override
        public String toString() {
            return "DRM{" +
                    posExp +
                    "," + negExp +
                    '}';
        }

        @Override
        public String getConfigString() {
            return toString();
        }
    }

    private final double posExp;
    private final double negExp;
    private long iteration = 1;

    private DiscountedRegretMatching(DiscountedRegretMatching rm) {
        super(rm);
        this.posExp = rm.posExp;
        this.negExp = rm.negExp;
        this.iteration = rm.iteration;
    }

    public DiscountedRegretMatching(int actionSize, double posExp, double negExp) {
        super(actionSize);
        this.posExp = posExp;
        this.negExp = negExp;
    }

    @Override
    protected double sumRegrets(double oldRegret, double regretDiff) {
        double exp = posExp;
        if (oldRegret < 0) {
            exp = negExp;
        }
        double mul;
        if (exp == Double.POSITIVE_INFINITY) {
            mul = 1;
        } else if (exp == Double.NEGATIVE_INFINITY) {
            mul = 0;
        } else {
            double tmp = Math.pow(iteration, exp);
            if (tmp == Double.POSITIVE_INFINITY) {
                mul = 1;
            } else {
                mul = tmp / (tmp + 1);
            }
        }

        return oldRegret * mul + regretDiff;
    }

    @Override
    public void getRegretMatchedStrategy(double[] probabilities) {
        super.getRegretMatchedStrategy(probabilities);
        iteration++;
    }

    @Override
    public IRegretMatching copy() {
        return new DiscountedRegretMatching(this);
    }
}

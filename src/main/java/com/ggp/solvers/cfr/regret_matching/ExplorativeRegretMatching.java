package com.ggp.solvers.cfr.regret_matching;

import com.ggp.solvers.cfr.IRegretMatching;

public class ExplorativeRegretMatching implements IRegretMatching {
    public static class Factory implements IRegretMatching.IFactory {
        private final IRegretMatching.IFactory rmFactory;
        private final double gamma;

        public Factory(IFactory rmFactory, double gamma) {
            if (rmFactory == null) {
                throw new IllegalArgumentException("Inner regret matching factory can't be null!");
            }
            this.rmFactory = rmFactory;
            this.gamma = gamma;
        }

        @Override
        public IRegretMatching create(int actionSize) {
            return new ExplorativeRegretMatching(rmFactory.create(actionSize), gamma);
        }

        @Override
        public String toString() {
            return "ERM{" +
                        rmFactory.getConfigString() +
                        "," + gamma +
                    '}';
        }

        @Override
        public String getConfigString() {
            return toString();
        }
    }

    private final IRegretMatching rm;
    private final double gamma;

    public ExplorativeRegretMatching(IRegretMatching rm, double gamma) {
        this.rm = rm;
        this.gamma = gamma;
    }

    @Override
    public void addActionRegret(int actionIdx, double regretDiff) {
        rm.addActionRegret(actionIdx, regretDiff);
    }

    @Override
    public void getRegretMatchedStrategy(double[] probabilities) {
        rm.getRegretMatchedStrategy(probabilities);
        final double unif = gamma/probabilities.length;
        for (int i = 0; i < probabilities.length; ++i) {
            probabilities[i] = unif + probabilities[i] * (1 - gamma);
        }
    }

    @Override
    public double getRegret(int actionIdx) {
        return rm.getRegret(actionIdx);
    }

    @Override
    public IRegretMatching copy() {
        return new ExplorativeRegretMatching(rm.copy(), gamma);
    }
}

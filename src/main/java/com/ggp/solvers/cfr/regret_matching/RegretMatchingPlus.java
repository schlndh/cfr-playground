package com.ggp.solvers.cfr.regret_matching;


import com.ggp.solvers.cfr.IRegretMatching;

public class RegretMatchingPlus extends BaseRegretMatching {
    public static class Factory implements IFactory {
        @Override
        public IRegretMatching create(int actionSize) {
            return new RegretMatchingPlus(actionSize);
        }

        @Override
        public String getConfigString() {
            return "RM+";
        }
    }

    public RegretMatchingPlus(int actionSize) {
        super(actionSize);
    }

    @Override
    protected double sumRegrets(double r1, double r2) {
        return Math.max(0, r1 + r2);
    }
}

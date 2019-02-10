package com.ggp.solvers.cfr.regret_matching;

import com.ggp.solvers.cfr.IRegretMatching;

public class RegretMatching extends BaseRegretMatching {
    public static class Factory implements IFactory {
        @Override
        public IRegretMatching create(int actionSize) {
            return new RegretMatching(actionSize);
        }

        @Override
        public String getConfigString() {
            return "RM";
        }
    }

    private RegretMatching(RegretMatching rm) {
        super(rm);
    }

    public RegretMatching(int actionSize) {
        super(actionSize);
    }

    @Override
    protected double sumRegrets(double r1, double r2) {
        return r1 + r2;
    }

    @Override
    public IRegretMatching copy() {
        return new RegretMatching(this);
    }
}

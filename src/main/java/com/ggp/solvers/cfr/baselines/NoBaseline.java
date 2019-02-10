package com.ggp.solvers.cfr.baselines;

import com.ggp.solvers.cfr.IBaseline;

public class NoBaseline implements IBaseline {
    public static class Factory implements IFactory {
        @Override
        public IBaseline create(int actionSize) {
            return new NoBaseline();
        }

        @Override
        public String getConfigString() {
            return "None";
        }
    }

    @Override
    public double getValue(int actionIdx) {
        return 0;
    }

    @Override
    public void update(int actionIdx, double utilityEstimate) {
    }

    @Override
    public IBaseline copy() {
        return this;
    }
}

package com.ggp.solvers.cfr.baselines;

import com.ggp.IAction;
import com.ggp.IInformationSet;
import com.ggp.solvers.cfr.IBaseline;

public class NoBaseline implements IBaseline {
    public static class Factory implements IFactory {
        @Override
        public IBaseline create() {
            return new NoBaseline();
        }

        @Override
        public String getConfigString() {
            return "None";
        }
    }

    @Override
    public double getValue(IInformationSet is, IAction a) {
        return 0;
    }

    @Override
    public void update(IInformationSet is, IAction a, double utilityEstimate) {
    }
}

package com.ggp.solvers.cfr.is_info;

import com.ggp.solvers.cfr.IRegretMatching;

import java.util.Arrays;

public class BaseCFRISInfo {
    protected IRegretMatching regretMatching;
    protected double[] strat;
    protected double[] cumulativeStrat;

    protected BaseCFRISInfo(BaseCFRISInfo info) {
        this.regretMatching = info.regretMatching.copy();
        this.strat = Arrays.copyOf(info.strat, info.strat.length);
        this.cumulativeStrat = Arrays.copyOf(info.cumulativeStrat, info.cumulativeStrat.length);
    }

    public BaseCFRISInfo(IRegretMatching.IFactory rmFactory, int actionSize) {
        this.regretMatching = rmFactory.create(actionSize);
        this.strat = new double[actionSize];
        this.cumulativeStrat = new double[actionSize];
        Arrays.fill(strat, 1d/actionSize);
        Arrays.fill(cumulativeStrat, 1d/actionSize);
    }

    public IRegretMatching getRegretMatching() {
        return regretMatching;
    }

    public double[] getStrat() {
        return strat;
    }

    public double[] getCumulativeStrat() {
        return cumulativeStrat;
    }

    public void doRegretMatching() {
        regretMatching.getRegretMatchedStrategy(strat);
    }

    public BaseCFRISInfo copy() {
        return new BaseCFRISInfo(this);
    }
}

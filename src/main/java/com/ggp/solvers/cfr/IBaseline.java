package com.ggp.solvers.cfr;

public interface IBaseline {
    interface IFactory {
        IBaseline create(int actionSize);
        String getConfigString();
    }

    double getValue(int actionIdx);
    void update(int actionIdx, double utilityEstimate);
    IBaseline copy();
}

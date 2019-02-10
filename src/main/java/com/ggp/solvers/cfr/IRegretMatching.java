package com.ggp.solvers.cfr;


public interface IRegretMatching {
    interface IFactory {
        IRegretMatching create(int actionSize);
        String getConfigString();
    }

    void addActionRegret(int actionIdx, double regretDiff);
    void getRegretMatchedStrategy(double[] probabilities);
    double getRegret(int actionIdx);
    IRegretMatching copy();
}

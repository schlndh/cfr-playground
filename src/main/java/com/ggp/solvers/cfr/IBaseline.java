package com.ggp.solvers.cfr;

import com.ggp.IAction;
import com.ggp.IInformationSet;

public interface IBaseline {
    interface IFactory {
        IBaseline create();
        String getConfigString();
    }

    double getValue(IInformationSet is, IAction a);
    void update(IInformationSet is, IAction a, double utilityEstimate);
}

package com.ggp.utils.strategy;

import com.ggp.IInfoSetStrategy;

public class NormalizingInfoSetStrategyWrapper implements IInfoSetStrategy {
    private static final long serialVersionUID = 1L;
    private IInfoSetStrategy unnormalizedIsStrat;
    private final double norm;

    public NormalizingInfoSetStrategyWrapper(IInfoSetStrategy unnormalizedIsStrat) {
        this.unnormalizedIsStrat = unnormalizedIsStrat;
        double n = 0;
        for (int i = 0; i < unnormalizedIsStrat.size(); ++i) {
            n += unnormalizedIsStrat.getProbability(i);
        }
        norm = n;
    }

    @Override
    public double getProbability(int actionIdx) {
        return unnormalizedIsStrat.getProbability(actionIdx)/norm;
    }

    @Override
    public int size() {
        return unnormalizedIsStrat.size();
    }
}

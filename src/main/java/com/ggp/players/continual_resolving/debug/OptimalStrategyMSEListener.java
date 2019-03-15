package com.ggp.players.continual_resolving.debug;

import com.ggp.players.continual_resolving.IResolvingInfo;
import com.ggp.utils.strategy.Strategy;
import com.ggp.utils.Metrics;

public class OptimalStrategyMSEListener extends BaseListener {
    private Strategy optimal;
    private int skipIterations = 5000;
    private int currentIteration;

    public OptimalStrategyMSEListener(Strategy optimal) {
        this.optimal = optimal;
    }

    @Override
    public void resolvingStart(IResolvingInfo resInfo) {
        currentIteration = 0;
    }

    @Override
    public void resolvingIterationEnd(IResolvingInfo resInfo) {
        if (!hasInitEnded()) return;
        currentIteration++;
        if (currentIteration % skipIterations == 1) {
            System.out.println(String.format("Optimal strategy MSE: %e", Metrics.getStrategyMSE(optimal, resInfo.getUnnormalizedCumulativeStrategy())));
        }
    }
}

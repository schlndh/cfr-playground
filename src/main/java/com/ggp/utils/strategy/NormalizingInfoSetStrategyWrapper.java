package com.ggp.utils.strategy;

import com.ggp.IInfoSetStrategy;

/**
 * Normalizing wrapper for infoset strategy.
 *
 * This class assumes the underlying strategy doesn't change while the wrapper is in use.
 * If the underlying strategy changes new wrapper must be created to reflect those changes.
 */
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
        if (norm > 0) return unnormalizedIsStrat.getProbability(actionIdx)/norm;
        return 1d/unnormalizedIsStrat.size();
    }

    @Override
    public int size() {
        return unnormalizedIsStrat.size();
    }
}

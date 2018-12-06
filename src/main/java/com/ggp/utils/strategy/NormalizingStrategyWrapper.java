package com.ggp.utils.strategy;

import com.ggp.IInfoSetStrategy;
import com.ggp.IInformationSet;
import com.ggp.IStrategy;

import java.util.HashMap;

/**
 * Normalizing wrapper for unnormalized strategy.
 *
 * This class assumes the underlying strategy doesn't change while the wrapper is in use.
 * If the underlying strategy changes new wrapper must be created to reflect those changes.
 */
public class NormalizingStrategyWrapper implements IStrategy {
    private static final long serialVersionUID = 1L;
    private IStrategy unnormalizedStrategy;
    private HashMap<IInformationSet, IInfoSetStrategy> isStrats = new HashMap<>();

    /**
     * Constructor
     * @param unnormalizedStrategy
     */
    public NormalizingStrategyWrapper(IStrategy unnormalizedStrategy) {
        this.unnormalizedStrategy = unnormalizedStrategy;
    }

    @Override
    public Iterable<IInformationSet> getDefinedInformationSets() {
        return unnormalizedStrategy.getDefinedInformationSets();
    }

    @Override
    public boolean isDefined(IInformationSet is) {
        return unnormalizedStrategy.isDefined(is);
    }

    @Override
    public IInfoSetStrategy getInfoSetStrategy(IInformationSet is) {
        return isStrats.computeIfAbsent(is, k -> new NormalizingInfoSetStrategyWrapper(unnormalizedStrategy.getInfoSetStrategy(is)));
    }
}

package com.ggp.utils.strategy;

import com.ggp.IInfoSetStrategy;
import com.ggp.IInformationSet;
import com.ggp.IStrategy;

import java.util.Set;

/**
 * Restricts strategy to given info sets.
 *
 * Returns uniform random strategy in other infosets.
 */
public class RestrictedStrategy implements IStrategy {
    private static final long serialVersionUID = 1L;
    private IStrategy innerStrategy;
    private Set<IInformationSet> allowedInfoSets;

    public RestrictedStrategy(IStrategy innerStrategy, Set<IInformationSet> allowedInfoSets) {
        this.innerStrategy = innerStrategy;
        this.allowedInfoSets = allowedInfoSets;
    }

    @Override
    public Iterable<IInformationSet> getDefinedInformationSets() {
        return allowedInfoSets;
    }

    @Override
    public boolean isDefined(IInformationSet is) {
        return allowedInfoSets.contains(is);
    }

    @Override
    public IInfoSetStrategy getInfoSetStrategy(IInformationSet is) {
        if (isDefined(is)) {
            return innerStrategy.getInfoSetStrategy(is);
        } else {
            return new UniformISStrategy(is.getLegalActions().size());
        }
    }
}

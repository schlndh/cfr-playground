package com.ggp.utils.strategy;

import com.ggp.IInfoSetStrategy;
import com.ggp.IInformationSet;
import com.ggp.IStrategy;

import java.util.HashSet;

/**
 * Replaces infoset strategies from the original with infoset strategies from the new strategy.
 *
 * Returns original infoset strategy for infosets which are undefined in the new strategy.
 */
public class ReplacedStrategy implements IStrategy {
    private IStrategy origStrat, replacingStrat;

    public ReplacedStrategy(IStrategy origStrat, IStrategy replacingStrat) {
        this.origStrat = origStrat;
        this.replacingStrat = replacingStrat;
    }

    @Override
    public Iterable<IInformationSet> getDefinedInformationSets() {
        HashSet<IInformationSet> infoSets = new HashSet<>();
        for (IInformationSet is: origStrat.getDefinedInformationSets()) {
            infoSets.add(is);
        }
        for (IInformationSet is: replacingStrat.getDefinedInformationSets()) {
            infoSets.add(is);
        }
        return infoSets;
    }

    @Override
    public boolean isDefined(IInformationSet is) {
        return replacingStrat.isDefined(is) || origStrat.isDefined(is);
    }

    @Override
    public IInfoSetStrategy getInfoSetStrategy(IInformationSet is) {
        if (replacingStrat.isDefined(is)) return replacingStrat.getInfoSetStrategy(is);
        return origStrat.getInfoSetStrategy(is);
    }
}

package com.ggp.utils.strategy;

import com.ggp.IInfoSetStrategy;
import com.ggp.IInformationSet;
import com.ggp.IStrategy;

import java.util.stream.StreamSupport;

/**
 * Limits the underlying strategy to given player.
 *
 * Infosets where the underlying strategy is not defined and other player's info sets return uniform random strategy.
 */
public class PlayerLimitedStrategy implements IStrategy {
    private IStrategy strat;
    private int myId;

    public PlayerLimitedStrategy(IStrategy strat, int myId) {
        this.strat = strat;
        this.myId = myId;
    }

    @Override
    public Iterable<IInformationSet> getDefinedInformationSets() {
        return () -> StreamSupport.stream(strat.getDefinedInformationSets().spliterator(), false).filter(is -> is.getOwnerId() == myId).iterator();
    }

    @Override
    public boolean isDefined(IInformationSet is) {
        if (is == null || is.getOwnerId() != myId) return false;
        return strat.isDefined(is);
    }

    @Override
    public IInfoSetStrategy getInfoSetStrategy(IInformationSet is) {
        if (is == null) return null;
        if (is.getOwnerId() != myId) return new UniformISStrategy(is.getLegalActions().size());
        return strat.getInfoSetStrategy(is);
    }
}

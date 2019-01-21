package com.ggp.players.deepstack;


import com.ggp.IInformationSet;
import com.ggp.IStrategy;
import com.ggp.utils.strategy.Strategy;

/**
 * Tracks current progress of resolving. Returned objects must not be modified.
 */
public interface IResolvingInfo {
    IStrategy getUnnormalizedCumulativeStrategy();
    IInformationSet getHiddenInfo();
    long getVisitedStatesInCurrentResolving();
}

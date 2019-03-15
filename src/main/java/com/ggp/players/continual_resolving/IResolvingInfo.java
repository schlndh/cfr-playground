package com.ggp.players.continual_resolving;


import com.ggp.IInformationSet;
import com.ggp.IStrategy;

/**
 * Tracks current progress of resolving. Returned objects must not be modified.
 */
public interface IResolvingInfo {
    IStrategy getUnnormalizedCumulativeStrategy();
    IInformationSet getHiddenInfo();
    long getVisitedStatesInCurrentResolving();
}

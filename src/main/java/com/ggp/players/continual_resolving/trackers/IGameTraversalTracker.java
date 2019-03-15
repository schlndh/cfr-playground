package com.ggp.players.continual_resolving.trackers;

import com.ggp.IAction;
import com.ggp.ICompleteInformationState;

public interface IGameTraversalTracker {
    IGameTraversalTracker next(IAction a);
    ICompleteInformationState getCurrentState();
    double getRndProb();
}

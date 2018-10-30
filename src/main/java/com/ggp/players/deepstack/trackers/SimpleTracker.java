package com.ggp.players.deepstack.trackers;

import com.ggp.IAction;
import com.ggp.ICompleteInformationState;
import com.ggp.IRandomNode;

public class SimpleTracker implements IGameTraversalTracker {
    private ICompleteInformationState state;
    private double rndProb;

    public SimpleTracker(ICompleteInformationState state, double rndProb) {
        this.state = state;
        this.rndProb = rndProb;
    }

    public static SimpleTracker createRoot(ICompleteInformationState state) {
        return new SimpleTracker(state, 1);
    }

    @Override
    public IGameTraversalTracker next(IAction a) {
        if (state.isRandomNode()) {
            IRandomNode rndNode = state.getRandomNode();
            double actionProb = rndNode.getActionProb(a);
            return new SimpleTracker(state.next(a), rndProb * actionProb);
        } else {
            return new SimpleTracker(state.next(a), rndProb);
        }
    }

    @Override
    public ICompleteInformationState getCurrentState() {
        return state;
    }

    @Override
    public double getRndProb() {
        return rndProb;
    }
}

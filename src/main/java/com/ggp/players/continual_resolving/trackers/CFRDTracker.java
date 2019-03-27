package com.ggp.players.continual_resolving.trackers;

import com.ggp.IAction;
import com.ggp.ICompleteInformationState;

public class CFRDTracker implements IGameTraversalTracker {
    private int myId;
    private double rndProb;
    private enum TrackingState {
        WAIT_MY_FIRST_TURN, WAIT_SUBGAME, WAIT_MY_NEXT_TURN, END
    }
    private TrackingState trackingState;
    private ICompleteInformationState state;
    private ICompleteInformationState lastSubgameRoot;

    private CFRDTracker(int myId, double rndProb, TrackingState trackingState, ICompleteInformationState state,
                       ICompleteInformationState lastSubgameRoot) {
        this.myId = myId;
        this.rndProb = rndProb;
        this.trackingState = trackingState;
        this.state = state;
        this.lastSubgameRoot = lastSubgameRoot;
    }

    public static CFRDTracker create(int myId, ICompleteInformationState root) {
        if (root == null || root.isTerminal()) return null;
        TrackingState trackingState = TrackingState.WAIT_MY_FIRST_TURN;
        if (root.getActingPlayerId() == myId) {
            trackingState = TrackingState.WAIT_SUBGAME;
        }
        return new CFRDTracker(myId, 1, trackingState, root, root);
    }

    private static boolean isChildSubgameRoot(ICompleteInformationState parent, ICompleteInformationState child) {
        // a child is at the root of a subgame if it has different acting player than the parent
        if (parent.isRandomNode() || child.isRandomNode() || child.isTerminal()) return false;
        return parent.getActingPlayerId() != child.getActingPlayerId();
    }

    private TrackingState getNextTrackingState(ICompleteInformationState nextState) {
        if (trackingState == TrackingState.WAIT_MY_FIRST_TURN && nextState.getActingPlayerId() == myId) {
            return TrackingState.WAIT_SUBGAME;
        }
        if (trackingState == TrackingState.WAIT_SUBGAME && isChildSubgameRoot(state, nextState)) {
            if (nextState.getActingPlayerId() == myId) return TrackingState.END;
            return TrackingState.WAIT_MY_NEXT_TURN;
        }
        if (trackingState == TrackingState.WAIT_MY_NEXT_TURN && nextState.getActingPlayerId() == myId) {
            return TrackingState.END;
        }
        return trackingState;
    }

    @Override
    public CFRDTracker next(IAction a) {
        ICompleteInformationState nextState = state.next(a);
        double newRndProb = rndProb;
        if (state.isRandomNode()) {
            newRndProb *= state.getRandomNode().getActionProb(a);
        }
        ICompleteInformationState newSubgameRoot = lastSubgameRoot;
        if (isChildSubgameRoot(state, nextState)) {
            newSubgameRoot = nextState;
        }
        return new CFRDTracker(myId, newRndProb, getNextTrackingState(nextState), nextState, newSubgameRoot);

    }

    public boolean wasMyFirstTurnReached() {
        return trackingState != TrackingState.WAIT_MY_FIRST_TURN;
    }

    public boolean wasMyNextTurnReached() {
        return trackingState == TrackingState.END;
    }

    @Override
    public ICompleteInformationState getCurrentState() {
        return state;
    }

    @Override
    public double getRndProb() {
        return rndProb;
    }

    public boolean isSubgameRoot() {
        return state.equals(lastSubgameRoot);
    }

    public ICompleteInformationState getLastSubgameRoot() {
        return lastSubgameRoot;
    }
}

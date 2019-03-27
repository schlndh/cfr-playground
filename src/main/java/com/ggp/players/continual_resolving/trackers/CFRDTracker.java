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
    private double utilityMultiplier = 1;

    private CFRDTracker(int myId, double rndProb, TrackingState trackingState, ICompleteInformationState state,
                       ICompleteInformationState lastSubgameRoot, double utilityMultiplier) {
        this.myId = myId;
        this.rndProb = rndProb;
        this.trackingState = trackingState;
        this.state = state;
        this.lastSubgameRoot = lastSubgameRoot;
        this.utilityMultiplier = utilityMultiplier;
    }

    public static CFRDTracker create(int myId, ICompleteInformationState root, double utilityMultiplier) {
        if (root == null || root.isTerminal()) return null;
        TrackingState trackingState = TrackingState.WAIT_MY_FIRST_TURN;
        if (root.getActingPlayerId() == myId) {
            trackingState = TrackingState.WAIT_SUBGAME;
        }
        return new CFRDTracker(myId, 1, trackingState, root, root, utilityMultiplier);
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
        return new CFRDTracker(myId, newRndProb, getNextTrackingState(nextState), nextState, newSubgameRoot, utilityMultiplier);

    }

    public boolean wasMyFirstTurnReached() {
        return trackingState != TrackingState.WAIT_MY_FIRST_TURN;
    }

    public boolean wasMyNextTurnReached() {
        return trackingState == TrackingState.END;
    }

    public boolean wasNextSubgameReached() {
        return trackingState.compareTo(TrackingState.WAIT_SUBGAME) > 0;
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

    @Override
    public double getPayoff(int player) {
        return state.getPayoff(player) * utilityMultiplier;
    }
}

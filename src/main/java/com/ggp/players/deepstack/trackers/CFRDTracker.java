package com.ggp.players.deepstack.trackers;

import com.ggp.IAction;
import com.ggp.ICompleteInformationState;
import com.ggp.IInformationSet;
import com.ggp.utils.PlayerHelpers;

public class CFRDTracker implements IGameTraversalTracker {
    private int myId;
    private double rndProb;
    private IAction myTopAction;
    private enum TrackingState {
        GAME_ROOT, CFRD_ROOT, MY_FIRST_TURN, WAIT_MY_TURN, END
    }
    private TrackingState trackingState;
    private TrackingState nextTrackingState;
    private ICompleteInformationState state;
    private IInformationSet myFirstIS;

    public CFRDTracker(int myId, double rndProb, IAction myTopAction, TrackingState trackingState,
                       ICompleteInformationState state, IInformationSet myFirstIS) {
        this.myId = myId;
        this.rndProb = rndProb;
        this.myTopAction = myTopAction;
        this.trackingState = trackingState;
        if (trackingState == TrackingState.GAME_ROOT && state.getActingPlayerId() == myId) {
            this.trackingState = TrackingState.MY_FIRST_TURN;
        }
        this.state = state;
        this.myFirstIS = myFirstIS;

        initNextState();
    }

    private void initNextState() {
        this.nextTrackingState = trackingState;
        if (trackingState == TrackingState.WAIT_MY_TURN && state.getActingPlayerId() == myId) {
            this.nextTrackingState = TrackingState.END;
        } else if (trackingState == TrackingState.MY_FIRST_TURN) {
            nextTrackingState = TrackingState.WAIT_MY_TURN;
            this.myFirstIS = state.getInfoSetForActingPlayer();
        } else if (trackingState == TrackingState.CFRD_ROOT && state.getActingPlayerId() == PlayerHelpers.getOpponentId(myId)) {
            nextTrackingState = TrackingState.MY_FIRST_TURN;
        }
    }

    public static CFRDTracker createForAct(int myId, ICompleteInformationState cfrdRoot) {
        return new CFRDTracker(myId, 1, null, TrackingState.CFRD_ROOT, cfrdRoot, null);
    }

    public static CFRDTracker createForInit(int myId, ICompleteInformationState state) {
        return new CFRDTracker(myId, 1, null, TrackingState.GAME_ROOT, state, null);
    }

    @Override
    public CFRDTracker next(IAction a) {
        ICompleteInformationState nextState = state.next(a);
        double newRndProb = rndProb;
        if (state.isRandomNode()) {
            newRndProb *= state.getRandomNode().getActionProb(a);
        }
        IAction newMyTopAction = myTopAction;
        if (trackingState == TrackingState.MY_FIRST_TURN && nextTrackingState == TrackingState.WAIT_MY_TURN) {
            newMyTopAction = a;
        }
        return new CFRDTracker(myId, newRndProb, newMyTopAction, nextTrackingState, nextState, myFirstIS);

    }
    public boolean isMyFirstTurnReached() {
        return trackingState == TrackingState.MY_FIRST_TURN;
    }

    public boolean isMyNextTurnReached() {
        return trackingState == TrackingState.WAIT_MY_TURN && nextTrackingState == TrackingState.END;
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

    public IAction getMyTopAction() {
        return myTopAction;
    }


    public IInformationSet getMyFirstIS() {
        return myFirstIS;
    }
}

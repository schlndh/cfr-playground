package com.ggp.players.continual_resolving.cfrd.AugmentedIS;

import com.ggp.IAction;
import com.ggp.ICompleteInformationState;
import com.ggp.utils.CompleteInformationStateWrapper;

public class CFRDAugmentedCISWrapper extends CompleteInformationStateWrapper {
    private final int opponentId;
    private final CFRDAugmentedIS opponentsAugmentedIS;


    public CFRDAugmentedCISWrapper(ICompleteInformationState state, int opponentId, CFRDAugmentedIS opponentsAugmentedIS) {
        super(state);
        this.opponentId = opponentId;
        this.opponentsAugmentedIS = opponentsAugmentedIS;
    }

    @Override
    public ICompleteInformationState next(IAction a) {
        ICompleteInformationState nextState = state.next(a);
        if (nextState.getActingPlayerId() == opponentId) {
            return new CFRDAugmentedCISWrapper(nextState, opponentId, new CFRDAugmentedIS(opponentId, nextState.getInfoSetForActingPlayer(), null));
        } else if (state.getActingPlayerId() == opponentId) {
            return new CFRDAugmentedCISWrapper(nextState, opponentId, new CFRDAugmentedIS(opponentId, state.getInfoSetForActingPlayer(), a));
        } else {
            return new CFRDAugmentedCISWrapper(nextState, opponentId, opponentsAugmentedIS);
        }
    }

    public CFRDAugmentedIS getOpponentsAugmentedIS() {
        return opponentsAugmentedIS;
    }

    @Override
    public String toString() {
        return "CFRDAugmentedCIS{" + state.toString() + "}";
    }
}
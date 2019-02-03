package com.ggp.players.deepstack.cfrd.AugmentedIS;

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
        if (state.getActingPlayerId() == opponentId) {
            return new CFRDAugmentedCISWrapper(state.next(a), opponentId, new CFRDAugmentedIS(opponentId, state.getInfoSetForActingPlayer(), a));
        } else {
            return new CFRDAugmentedCISWrapper(state.next(a), opponentId, opponentsAugmentedIS);
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
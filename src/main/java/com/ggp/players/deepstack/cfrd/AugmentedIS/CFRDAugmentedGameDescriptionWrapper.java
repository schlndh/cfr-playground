package com.ggp.players.deepstack.cfrd.AugmentedIS;

import com.ggp.ICompleteInformationState;
import com.ggp.IGameDescription;

public class CFRDAugmentedGameDescriptionWrapper implements IGameDescription {
    private final IGameDescription gameDesc;
    private final int opponentId;

    public CFRDAugmentedGameDescriptionWrapper(IGameDescription gameDesc, int opponentId) {
        this.gameDesc = gameDesc;
        this.opponentId = opponentId;
    }

    @Override
    public ICompleteInformationState getInitialState() {
        return new CFRDAugmentedCISWrapper(gameDesc.getInitialState(), opponentId, new CFRDAugmentedIS(opponentId, null, null));
    }

    @Override
    public String getConfigString() {
        return gameDesc.getConfigString();
    }

    @Override
    public String toString() {
        return "CFRDAugmented{" + gameDesc.toString() + "}";
    }
}

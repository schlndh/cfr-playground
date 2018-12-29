package com.ggp.games.LatentTicTacToe;

import com.ggp.ICompleteInformationState;
import com.ggp.IGameDescription;

public class GameDescription implements IGameDescription {
    private static final long serialVersionUID = 1L;
    @Override
    public ICompleteInformationState getInitialState() {
        return new CompleteInformationState(
                new InformationSet(new int[InformationSet.GRID_SIZE*InformationSet.GRID_SIZE], 1, 0, 0, 0, null),
                new InformationSet(new int[InformationSet.GRID_SIZE*InformationSet.GRID_SIZE], 2, 0, 0, 0, null),
                1
            );
    }

    @Override
    public String getConfigString() {
        return "LatentTicTacToe{}";
    }
}

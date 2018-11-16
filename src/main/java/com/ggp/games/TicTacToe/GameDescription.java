package com.ggp.games.TicTacToe;

import com.ggp.ICompleteInformationState;
import com.ggp.ICompleteInformationStateFactory;
import com.ggp.IGameDescription;
import com.ggp.IInformationSet;

import java.util.Objects;

public class GameDescription implements IGameDescription {
    private int size = 5;
    private static final long serialVersionUID = 1L;
    private static CompleteInformationState initialState;
    static {
        int field[] = {
                0, 0, 0, 0, 0,
                0, 0, 0, 0, 0,
                0, 0, 0, 0, 0,
                0, 0, 0, 0, 0,
                0, 0, 0, 0, 0,
        };
        InformationSet xInfoSet = new InformationSet(field, CompleteInformationState.PLAYER_X, 0, 0, 0);
        InformationSet oInfoSet = new InformationSet(field, CompleteInformationState.PLAYER_O, 0, 0, 0);
        initialState = new CompleteInformationState(xInfoSet, oInfoSet, CompleteInformationState.PLAYER_X);
    }

    @Override
    public ICompleteInformationState getInitialState() {
        return initialState;
    }

    public ICompleteInformationStateFactory getCISFactory() {
        return new ICompleteInformationStateFactory() {
            @Override
            public ICompleteInformationState make(IInformationSet player1, IInformationSet player2, int actingPlayer) {
                // TODO: check if both IS are compatible with each other
                return new CompleteInformationState((InformationSet) player1, (InformationSet) player2, actingPlayer);
            }
        };
    }

    @Override
    public String toString() {
        return "KriegTicTacToe{N=5}";
    }

    @Override
    public String getConfigString() {
        return toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GameDescription that = (GameDescription) o;
        return size == that.size;
    }

    @Override
    public int hashCode() {
        return Objects.hash(size);
    }
}

package com.ggp.games.IIGoofspiel;

import com.ggp.ICompleteInformationState;
import com.ggp.ICompleteInformationStateFactory;
import com.ggp.IGameDescription;
import com.ggp.utils.recall.PerfectRecallGame;

import java.util.Objects;

@PerfectRecallGame
public class GameDescription implements IGameDescription {
    private static final long serialVersionUID = 1L;
    private CompleteInformationState initialState;

    public GameDescription(int gameSize) {
        initialState = new CompleteInformationState(
                new InformationSet(1, gameSize, null, null),
                new InformationSet(2, gameSize, null, null)
        );
    }

    @Override
    public ICompleteInformationState getInitialState() {
        return initialState;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GameDescription that = (GameDescription) o;
        return Objects.equals(initialState.getGameSize(), that.initialState.getGameSize());
    }

    @Override
    public int hashCode() {
        return Objects.hash(initialState.getGameSize());
    }

    @Override
    public String toString() {
        return "IIGoofspiel{" +
                    initialState.getGameSize() +
                '}';
    }

    @Override
    public String getConfigString() {
        return toString();
    }
}

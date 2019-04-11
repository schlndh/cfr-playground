package com.ggp.utils.game.CheckedTraversal;

import com.ggp.ICompleteInformationState;
import com.ggp.IGameDescription;

import java.util.Objects;

public class CheckedTraversalGameDescription implements IGameDescription {
    private static final long serialVersionUID = 1L;
    private final IGameDescription gameDesc;

    public CheckedTraversalGameDescription(IGameDescription gameDesc) {
        this.gameDesc = gameDesc;
    }

    @Override
    public ICompleteInformationState getInitialState() {
        return new CheckedTraversalCISWrapper(gameDesc.getInitialState());
    }

    @Override
    public String toString() {
        return "CheckedTraversal{" +
                    gameDesc +
                '}';
    }

    @Override
    public String getConfigString() {
        return toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CheckedTraversalGameDescription that = (CheckedTraversalGameDescription) o;
        return Objects.equals(gameDesc, that.gameDesc);
    }

    @Override
    public int hashCode() {
        return Objects.hash(gameDesc);
    }
}

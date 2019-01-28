package com.ggp.utils.recall;

import com.ggp.*;

import java.util.Objects;

/**
 * Makes a perfect-recall version of given game.
 */
@PerfectRecallGame
public class PerfectRecallGameDescriptionWrapper implements IGameDescription {
    private static final long serialVersionUID = 1L;
    private IGameDescription gameDesc;
    private PerfectRecallCIS initialState;

    public PerfectRecallGameDescriptionWrapper(IGameDescription gameDesc) {
        this.gameDesc = gameDesc;
        ICompleteInformationState s = gameDesc.getInitialState();
        initialState = (PerfectRecallCIS) wrapInitialState(s);
    }

    public static ICompleteInformationState wrapInitialState(ICompleteInformationState initialState) {
        return new PerfectRecallCIS(initialState, new PerfectRecallIS(initialState.getInfoSetForPlayer(1), null),
                new PerfectRecallIS(initialState.getInfoSetForPlayer(2), null));
    }

    @Override
    public ICompleteInformationState getInitialState() {
        return initialState;
    }

    @Override
    public String toString() {
        return "PerfectRecall{" +
                    gameDesc.getConfigString() +
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
        PerfectRecallGameDescriptionWrapper that = (PerfectRecallGameDescriptionWrapper) o;
        return Objects.equals(gameDesc, that.gameDesc);
    }

    @Override
    public int hashCode() {
        return Objects.hash(gameDesc);
    }
}

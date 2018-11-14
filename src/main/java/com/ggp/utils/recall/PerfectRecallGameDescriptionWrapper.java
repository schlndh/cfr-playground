package com.ggp.utils.recall;

import com.ggp.*;

/**
 * Makes a perfect-recall version of given game.
 */
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
}

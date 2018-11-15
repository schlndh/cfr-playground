package com.ggp;

import java.io.Serializable;

public interface IGameDescription extends Serializable {
    ICompleteInformationState getInitialState();
    String getConfigString();
    default IInformationSet getInitialInformationSet(int role) {
        return getInitialState().getInfoSetForPlayer(role);
    }
}

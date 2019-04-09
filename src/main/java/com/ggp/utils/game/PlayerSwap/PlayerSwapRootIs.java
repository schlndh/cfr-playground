package com.ggp.utils.game.PlayerSwap;

import com.ggp.IAction;
import com.ggp.ICompleteInformationState;
import com.ggp.IInformationSet;
import com.ggp.IPercept;
import com.ggp.utils.PlayerHelpers;

import java.util.List;

public class PlayerSwapRootIs implements IInformationSet {
    private static final long serialVersionUID = 1L;
    private final ICompleteInformationState gameRoot;
    private final int owner;

    public PlayerSwapRootIs(ICompleteInformationState gameRoot, int owner) {
        this.gameRoot = gameRoot;
        this.owner = owner;
    }

    @Override
    public IInformationSet next(IAction a) {
        return null;
    }

    @Override
    public IInformationSet applyPercept(IPercept p) {
        if (!isValid(p)) return null;
        if (((PlayerSwapPercept)p).isSwapped()) return new PlayerSwapISWrapper(gameRoot.getInfoSetForPlayer(PlayerHelpers.getOpponentId(owner)));
        return gameRoot.getInfoSetForPlayer(owner);
    }

    @Override
    public List<IAction> getLegalActions() {
        return null;
    }

    @Override
    public boolean isLegal(IAction a) {
        return false;
    }

    @Override
    public boolean isValid(IPercept p) {
        return p.getClass() == PlayerSwapPercept.class;
    }

    @Override
    public int getOwnerId() {
        return owner;
    }
}

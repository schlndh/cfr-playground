package com.ggp.utils.game.PlayerSwap;

import com.ggp.IAction;
import com.ggp.IInformationSet;
import com.ggp.IPercept;
import com.ggp.utils.InformationSetWrapper;
import com.ggp.utils.PlayerHelpers;

public class PlayerSwapISWrapper extends InformationSetWrapper {
    private static final long serialVersionUID = 1L;
    public PlayerSwapISWrapper(IInformationSet infoSet) {
        super(infoSet);
    }

    @Override
    public IInformationSet next(IAction a) {
        return new PlayerSwapISWrapper(infoSet.next(a));
    }

    @Override
    public boolean isValid(IPercept p) {
        if (p == null || p.getClass() != PlayerSwapPerceptWrapper.class) return false;
        return infoSet.isValid(((PlayerSwapPerceptWrapper)p).getPercept());
    }

    @Override
    public IInformationSet applyPercept(IPercept p) {
        if (p == null || p.getClass() != PlayerSwapPerceptWrapper.class) return null;
        return new PlayerSwapISWrapper(infoSet.applyPercept(((PlayerSwapPerceptWrapper)p).getPercept()));
    }

    @Override
    public int getOwnerId() {
        return PlayerHelpers.getOpponentId(super.getOwnerId());
    }

    @Override
    public String toString() {
        return "PlayerSwap{" +
                    infoSet +
                '}';
    }
}

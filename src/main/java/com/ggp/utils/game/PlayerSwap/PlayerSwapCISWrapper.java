package com.ggp.utils.game.PlayerSwap;

import com.ggp.IAction;
import com.ggp.ICompleteInformationState;
import com.ggp.IInformationSet;
import com.ggp.IPercept;
import com.ggp.utils.CompleteInformationStateWrapper;
import com.ggp.utils.PlayerHelpers;

import java.util.stream.StreamSupport;

public class PlayerSwapCISWrapper extends CompleteInformationStateWrapper {
    private static final long serialVersionUID = 1L;

    public PlayerSwapCISWrapper(ICompleteInformationState state) {
        super(state);
    }

    @Override
    public IInformationSet getInfoSetForPlayer(int player) {
        return new PlayerSwapISWrapper(super.getInfoSetForPlayer(PlayerHelpers.getOpponentId(player)));
    }

    @Override
    public ICompleteInformationState next(IAction a) {
        return new PlayerSwapCISWrapper(state.next(a));
    }

    @Override
    public int getActingPlayerId() {
        if (state.isRandomNode()) return 0;
        return PlayerHelpers.getOpponentId(super.getActingPlayerId());
    }

    @Override
    public double getPayoff(int player) {
        if (!isTerminal()) return 0;
        return super.getPayoff(PlayerHelpers.getOpponentId(player));
    }

    @Override
    public Iterable<IPercept> getPercepts(IAction a) {
        Iterable<IPercept> ret = super.getPercepts(a);
        if (ret == null) return null;
        return () -> StreamSupport.stream(ret.spliterator(), false).map(p -> (IPercept)new PlayerSwapPerceptWrapper(p)).iterator();
    }

    @Override
    public String toString() {
        return "PlayerSwap{" +
                    state +
                '}';
    }
}

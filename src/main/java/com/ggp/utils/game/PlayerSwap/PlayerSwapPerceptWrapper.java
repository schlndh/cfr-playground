package com.ggp.utils.game.PlayerSwap;

import com.ggp.IPercept;
import com.ggp.utils.PlayerHelpers;

import java.util.Objects;

public class PlayerSwapPerceptWrapper implements IPercept {
    private static final long serialVersionUID = 1L;
    private final IPercept percept;

    public PlayerSwapPerceptWrapper(IPercept percept) {
        this.percept = percept;
    }

    @Override
    public int getTargetPlayer() {
        return PlayerHelpers.getOpponentId(percept.getTargetPlayer());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PlayerSwapPerceptWrapper that = (PlayerSwapPerceptWrapper) o;
        return Objects.equals(percept, that.percept);
    }

    @Override
    public int hashCode() {
        return Objects.hash(percept);
    }

    @Override
    public String toString() {
        return "PlayerSwap{" +
                    percept +
                '}';
    }

    public IPercept getPercept() {
        return percept;
    }
}

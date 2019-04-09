package com.ggp.utils.game.PlayerSwap;

import com.ggp.IPercept;

import java.util.Objects;

public class PlayerSwapPercept implements IPercept {
    private static final long serialVersionUID = 1L;
    private int owner;
    private boolean isSwapped;

    public PlayerSwapPercept(int owner, boolean isSwapped) {
        this.owner = owner;
        this.isSwapped = isSwapped;
    }

    @Override
    public int getTargetPlayer() {
        return owner;
    }

    public boolean isSwapped() {
        return isSwapped;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PlayerSwapPercept that = (PlayerSwapPercept) o;
        return owner == that.owner &&
                isSwapped == that.isSwapped;
    }

    @Override
    public int hashCode() {
        return Objects.hash(owner, isSwapped);
    }
}

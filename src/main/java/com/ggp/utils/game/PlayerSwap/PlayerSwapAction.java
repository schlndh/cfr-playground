package com.ggp.utils.game.PlayerSwap;

import com.ggp.IAction;

import java.util.Objects;

public class PlayerSwapAction implements IAction {
    private static final long serialVersionUID = 1L;
    private final boolean doSwap;
    public static final PlayerSwapAction YES = new PlayerSwapAction(true);
    public static final PlayerSwapAction NO = new PlayerSwapAction(false);

    private PlayerSwapAction(boolean doSwap) {
        this.doSwap = doSwap;
    }

    public boolean isDoSwap() {
        return doSwap;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PlayerSwapAction that = (PlayerSwapAction) o;
        return doSwap == that.doSwap;
    }

    @Override
    public int hashCode() {
        return Objects.hash(doSwap);
    }
}

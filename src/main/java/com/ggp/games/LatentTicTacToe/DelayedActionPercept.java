package com.ggp.games.LatentTicTacToe;

import com.ggp.IPercept;

import java.util.Objects;

public class DelayedActionPercept implements IPercept {
    private static final long serialVersionUID = 1L;
    private int owner;
    private MarkFieldAction delayedAction;

    public DelayedActionPercept(int owner, MarkFieldAction delayedAction) {
        this.owner = owner;
        this.delayedAction = delayedAction;
    }

    @Override
    public int getTargetPlayer() {
        return owner;
    }

    public MarkFieldAction getDelayedAction() {
        return delayedAction;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DelayedActionPercept that = (DelayedActionPercept) o;
        return owner == that.owner &&
                Objects.equals(delayedAction, that.delayedAction);
    }

    @Override
    public int hashCode() {
        return Objects.hash(owner, delayedAction);
    }
}

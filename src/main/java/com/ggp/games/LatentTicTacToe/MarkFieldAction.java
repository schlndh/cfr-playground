package com.ggp.games.LatentTicTacToe;

import com.ggp.IAction;

import java.util.Objects;

public class MarkFieldAction implements IAction {
    private static final long serialVersionUID = 1L;

    private int x;
    private int y;

    public MarkFieldAction(int x, int y) {
        this.x = x;
        this.y = y;
    }


    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    @Override
    public String toString() {
        return "Mark{" +
                x +
                "," + y +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MarkFieldAction that = (MarkFieldAction) o;
        return x == that.x &&
                y == that.y;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }
}

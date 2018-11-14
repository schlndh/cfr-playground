package com.ggp.games.IIGoofspiel;

import com.ggp.IPercept;

import java.util.Objects;

public class BetResultPercept implements IPercept {
    private static final long serialVersionUID = 1L;
    private final int owner;
    private final int card;
    private final int winner;

    public BetResultPercept(int owner, int card, int winner) {
        this.owner = owner;
        this.card = card;
        this.winner = winner;
    }

    @Override
    public int getTargetPlayer() {
        return owner;
    }

    public int getCard() {
        return card;
    }

    public int getWinner() {
        return winner;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BetResultPercept that = (BetResultPercept) o;
        return owner == that.owner &&
                card == that.card &&
                winner == that.winner;
    }

    @Override
    public int hashCode() {
        return Objects.hash(owner, card, winner);
    }

    @Override
    public String toString() {
        return "BetResult{" +
                "c=" + card +
                ", w=" + winner +
                '}';
    }
}

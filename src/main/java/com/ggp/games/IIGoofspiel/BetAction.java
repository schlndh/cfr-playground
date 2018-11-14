package com.ggp.games.IIGoofspiel;

import com.ggp.IAction;

import java.util.Objects;

public class BetAction implements IAction {
    private static final long serialVersionUID = 1L;
    private final int owner;
    private final int card;

    public BetAction(int owner, int card) {
        this.owner = owner;
        this.card = card;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BetAction betAction = (BetAction) o;
        return owner == betAction.owner &&
                card == betAction.card;
    }

    @Override
    public int hashCode() {
        return Objects.hash(owner, card);
    }

    @Override
    public String toString() {
        return "Bet{" + card + '}';
    }

    public int getOwner() {
        return owner;
    }

    public int getCard() {
        return card;
    }
}

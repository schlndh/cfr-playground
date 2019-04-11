package com.ggp.games.IIGoofspiel;

import com.ggp.*;
import com.ggp.utils.PlayerHelpers;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class CompleteInformationState implements ICompleteInformationState {
    private static final long serialVersionUID = 1L;
    private final InformationSet player1IS, player2IS;

    public CompleteInformationState(InformationSet player1IS, InformationSet player2IS) {
        this.player1IS = player1IS;
        this.player2IS = player2IS;
    }

    @Override
    public boolean isTerminal() {
        return player2IS.getGameSize() == player2IS.getRound();
    }

    @Override
    public int getActingPlayerId() {
        if (player1IS.getRound() == player2IS.getRound()) return 1;
        return 2;
    }

    @Override
    public double getPayoff(int player) {
        if (!isTerminal()) return 0;
        return (double) PlayerHelpers.callWithOrderedParams(player, player1IS.getScore(), player2IS.getScore(), (myScore, oppScore) -> {
            if (myScore > oppScore) return 1;
            if (oppScore > myScore) return -1;
            return 0;
        });
    }

    @Override
    public List<IAction> getLegalActions() {
        return getInfoSetForActingPlayer().getLegalActions();
    }

    @Override
    public IInformationSet getInfoSetForPlayer(int player) {
        return PlayerHelpers.selectByPlayerId(player, player1IS, player2IS);
    }

    private int getWinner(int card1, int card2) {
        int winner = 0;
        if (card1 > card2) {
            winner = 1;
        } else if (card2 > card1) {
            winner = 2;
        }
        return winner;
    }

    @Override
    public ICompleteInformationState next(IAction a) {
        InformationSet newP1IS = player1IS, newP2IS = player2IS;
        if (getActingPlayerId() == 1) {
            newP1IS = (InformationSet) player1IS.next(a);
        } else {
            BetAction b = (BetAction) a;
            newP2IS = (InformationSet) player2IS.next(a);
            int winner = getWinner(player1IS.getLastUsedCard(), b.getCard());
            int card = player1IS.getRound();
            newP1IS = (InformationSet) newP1IS.applyPercept(new BetResultPercept(1, card, winner));
            newP2IS = (InformationSet) newP2IS.applyPercept(new BetResultPercept(2, card, winner));
        }
        return new CompleteInformationState(newP1IS, newP2IS);
    }

    @Override
    public Iterable<IPercept> getPercepts(IAction a) {
        if (getActingPlayerId() == 1) return Collections.EMPTY_LIST;
        BetAction b = (BetAction) a;
        int winner = getWinner(player1IS.getLastUsedCard(), b.getCard());
        int card = player1IS.getRound();
        return Arrays.asList(new BetResultPercept(1, card, winner), new BetResultPercept(2, card, winner));
    }

    @Override
    public IRandomNode getRandomNode() {
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CompleteInformationState that = (CompleteInformationState) o;
        return Objects.equals(player1IS, that.player1IS) &&
                Objects.equals(player2IS, that.player2IS);
    }

    @Override
    public int hashCode() {
        return Objects.hash(player1IS, player2IS);
    }

    @Override
    public String toString() {
        StringBuilder gameplay = new StringBuilder();
        for (int i = 0; i < player1IS.getRound(); ++i) {
            int c1 = player1IS.getCardFromRound(i), c2 = player2IS.getCardFromRound(i);
            gameplay.append('(');
            gameplay.append((c1 < 0 ? "?" : Integer.toString(c1)));
            gameplay.append(',');
            gameplay.append((c2 < 0 ? "?" : Integer.toString(c2)));
            gameplay.append(")|");
        }
        return "CIS{" +
                "turn=" + getActingPlayerId() +
                ", p1Score=" + player1IS.getScore() +
                ", p2Score=" + player2IS.getScore() +
                ", gameplay=" + gameplay.toString() +
                '}';
    }

    public int getGameSize() {
        return player1IS.getGameSize();
    }
}

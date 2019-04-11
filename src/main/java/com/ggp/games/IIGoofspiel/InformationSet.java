package com.ggp.games.IIGoofspiel;

import com.ggp.IAction;
import com.ggp.IInformationSet;
import com.ggp.IPercept;

import java.io.IOException;
import java.util.*;

public class InformationSet implements IInformationSet {
    private static final long serialVersionUID = 1L;
    private final int owner;
    private final int gameSize;
    private final int[] usedCards;
    // W = win, L = loss, T = tie
    private final char[] wins;
    private transient int[] cardRounds;
    private transient List<IAction> legalActions;

    public InformationSet(int owner, int gameSize, int[] usedCards, char[] wins) {
        this.owner = owner;
        this.gameSize = gameSize;
        this.usedCards = usedCards;
        this.wins = wins;
        init();
    }

    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        init();
    }

    private void init() {
        if (usedCards != null) {
            cardRounds = new int[gameSize];
            Arrays.fill(cardRounds, -1);
            for (int i = 0; i < usedCards.length; i++) {
                cardRounds[usedCards[i]] = i;
            }
        }
        ArrayList<IAction> actions = new ArrayList<>(gameSize - (usedCards != null ? usedCards.length : 0));
        for (int card = 0; card < gameSize; ++card) {
            if (cardRounds == null || cardRounds[card] < 0) actions.add(new BetAction(owner, card));
        }
        legalActions = Collections.unmodifiableList(actions);
    }

    @Override
    public IInformationSet next(IAction a) {
        BetAction b = (BetAction) a;
        int[] newUsedCards = (usedCards != null ? Arrays.copyOf(usedCards, usedCards.length + 1) : new int[1]);
        newUsedCards[newUsedCards.length - 1] = b.getCard();
        // wins will be updated once the percept is received
        return new InformationSet(owner, gameSize, newUsedCards, wins);
    }

    @Override
    public IInformationSet applyPercept(IPercept p) {
        BetResultPercept b = (BetResultPercept) p;
        char[] newWins = (wins != null ? Arrays.copyOf(wins, wins.length + 1) : new char[1]);
        newWins[newWins.length - 1] = b.getWinner() == owner ? 'W' : (b.getWinner() == 0  ? 'T' : 'L');
        return new InformationSet(owner, gameSize, usedCards, newWins);
    }

    @Override
    public List<IAction> getLegalActions() {
        return legalActions;
    }

    @Override
    public boolean isLegal(IAction a) {
        if(a == null || a.getClass() != BetAction.class) return false;
        BetAction b = (BetAction)a;
        if (b.getOwner() != owner) return false;
        if (cardRounds != null && cardRounds[b.getCard()] >= 0) return false;
        return true;
    }

    @Override
    public boolean isValid(IPercept p) {
        if (p == null || p.getClass() !=  BetResultPercept.class) return false;
        BetResultPercept b = (BetResultPercept) p;
        return b.getTargetPlayer() == owner && b.getCard() == getRound();
    }

    @Override
    public int getOwnerId() {
        return owner;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InformationSet that = (InformationSet) o;
        return owner == that.owner &&
                gameSize == that.gameSize &&
                Arrays.equals(usedCards, that.usedCards) &&
                Arrays.equals(wins, that.wins);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(owner, gameSize);
        result = 31 * result + Arrays.hashCode(usedCards);
        result = 31 * result + Arrays.hashCode(wins);
        return result;
    }

    @Override
    public String toString() {
        StringBuilder gameplay = new StringBuilder();
        if (usedCards != null) {
            for (int i = 0; i < usedCards.length; i++) {
                int c = usedCards[i];
                char r;
                if (wins != null && i < wins.length) r = wins[i];
                else r = '?';
                gameplay.append(c);
                gameplay.append(':');
                gameplay.append(r);
                gameplay.append(',');
            }
        }
        return "InformationSet{" +
                "owner=" + owner +
                ", gameSize=" + gameSize +
                ", gameplay=" + gameplay.toString() +
                '}';
    }

    public int getGameSize() {
        return gameSize;
    }

    public int getRound() {
        if (usedCards == null) return 0;
        return usedCards.length;
    }

    public int getScore() {
        int score = 0;
        if (wins != null) {
            for (int i = 0; i < wins.length; i++) {
                if (wins[i] == 'W') score += i + 1;
            }
        }
        return score;
    }

    public int getLastUsedCard() {
        if (usedCards == null) return -1;
        return usedCards[usedCards.length - 1];
    }

    public int getCardFromRound(int round) {
        if (usedCards == null || round < 0 || round >= usedCards.length) return -1;
        return usedCards[round];
    }
}

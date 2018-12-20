package com.ggp.games.LeducPoker;

import com.ggp.ICompleteInformationState;
import com.ggp.ICompleteInformationStateFactory;
import com.ggp.IGameDescription;
import com.ggp.IInformationSet;

import java.util.Arrays;
import java.util.Objects;

public class GameDescription implements IGameDescription {
    private static final long serialVersionUID = 1L;
    private final int[] startingMoney = new int[] {0,0};
    private final int betsPerRound;
    private final CompleteInformationState initialState;

    public GameDescription(int startingMoney) {
        this(startingMoney, startingMoney, 1);
    }

    public GameDescription(int startingMoney1, int startingMoney2, int betsPerRound) {
        this.startingMoney[0] = startingMoney1;
        this.startingMoney[1] = startingMoney2;
        this.betsPerRound = betsPerRound;
        InformationSet player1IS = new InformationSet(this,1, null, null, 2, startingMoney1 - 1, Rounds.PrivateCard, 0, 0);
        InformationSet player2IS = new InformationSet(this,2, null, null, 2, startingMoney2 - 1, Rounds.PrivateCard, 0, 0);
        initialState = new CompleteInformationState(player1IS, player2IS, 0);
    }

    @Override
    public ICompleteInformationState getInitialState() {
        return initialState;
    }

    @Override
    public String toString() {
        return "LeducPoker{" +
                    startingMoney[0] +
                    "," + startingMoney[1] +
                    "," + betsPerRound +
                '}';
    }

    @Override
    public String getConfigString() {
        return toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GameDescription that = (GameDescription) o;
        return betsPerRound == that.betsPerRound &&
                Arrays.equals(startingMoney, that.startingMoney);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(betsPerRound);
        result = 31 * result + Arrays.hashCode(startingMoney);
        return result;
    }

    public int getStartingMoney(int player) {
        return startingMoney[player - 1];
    }

    public int getBetsPerRound() {
        return betsPerRound;
    }
}

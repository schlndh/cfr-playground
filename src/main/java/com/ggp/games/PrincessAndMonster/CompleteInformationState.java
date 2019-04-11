package com.ggp.games.PrincessAndMonster;

import com.ggp.*;
import com.ggp.utils.PlayerHelpers;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class CompleteInformationState implements ICompleteInformationState {
    private static final long serialVersionUID = 1L;
    private final InformationSet princessIS, monsterIS;

    public CompleteInformationState(InformationSet princessIS, InformationSet monsterIS) {
        this.princessIS = princessIS;
        this.monsterIS = monsterIS;
    }

    public GameDescription getGameDesc() {
        return princessIS.getGameDesc();
    }

    @Override
    public boolean isTerminal() {
        GameDescription gameDesc = getGameDesc();
        return gameDesc.getMaxTime() == monsterIS.getMyTurns() || princessIS.getMyPosition().equals(monsterIS.getMyPosition());
    }

    @Override
    public int getActingPlayerId() {
        if (princessIS.getMyTurns() == monsterIS.getMyTurns()) return 1;
        return 2;
    }

    @Override
    public double getPayoff(int player) {
        if (!isTerminal()) return 0;
        GameDescription gameDesc = getGameDesc();
        double princessPayoff = 0;
        // monster won
        if (princessIS.getMyPosition().equals(monsterIS.getMyPosition())) {
            princessPayoff = -(2 * gameDesc.getMaxTime() - princessIS.getMyTurns() - monsterIS.getMyTurns());
        } else {
            princessPayoff = 2 * gameDesc.getMaxTime();
        }
        return PlayerHelpers.selectByPlayerId(player, princessPayoff, -princessPayoff);
    }

    @Override
    public List<IAction> getLegalActions() {
        return getInfoSetForActingPlayer().getLegalActions();
    }

    @Override
    public IInformationSet getInfoSetForPlayer(int player) {
        return PlayerHelpers.selectByPlayerId(player, princessIS, monsterIS);
    }

    @Override
    public ICompleteInformationState next(IAction a) {
        InformationSet is1 = princessIS, is2 = monsterIS;
        if (getActingPlayerId() == 1) {
            is1 = (InformationSet) is1.next(a);
        } else {
            is2 = (InformationSet) is2.next(a);
        }
        return new CompleteInformationState(is1, is2);
    }

    @Override
    public Iterable<IPercept> getPercepts(IAction a) {
        return Collections.EMPTY_LIST;
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
        return Objects.equals(princessIS, that.princessIS) &&
                Objects.equals(monsterIS, that.monsterIS);
    }

    @Override
    public int hashCode() {
        return Objects.hash(princessIS, monsterIS);
    }

    @Override
    public String toString() {
        return "CIS{" +
                "princess=" + princessIS +
                ", monster=" + monsterIS +
                '}';
    }
}

package com.ggp.games.LatentTicTacToe;

import com.ggp.*;
import com.ggp.utils.PlayerHelpers;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class CompleteInformationState implements ICompleteInformationState {
    private static final long serialVersionUID = 1L;
    private InformationSet xInfoSet;
    private InformationSet oInfoSet;
    private boolean terminal;
    private int actingPlayer;
    private int xPayoff = 0;

    public static final int PLAYER_X = 1;
    public static final int PLAYER_O = 2;

    public CompleteInformationState(InformationSet xInfoSet, InformationSet oInfoSet, int actingPlayer) {
        this.xInfoSet = xInfoSet;
        this.oInfoSet = oInfoSet;
        this.actingPlayer = actingPlayer;
        detectTerminal();
    }

    @Override
    public boolean isTerminal() {
        return terminal;
    }

    @Override
    public boolean isRandomNode() {
        return false;
    }

    @Override
    public int getActingPlayerId() {
        return actingPlayer;
    }

    @Override
    public double getPayoff(int player) {
        if (!isTerminal()) return 0;
        if (player == PLAYER_X) return xPayoff;
        else return -xPayoff;
    }

    @Override
    public List<IAction> getLegalActions() {
        return getActingPlayerInfoSet().getLegalActions();
    }

    @Override
    public ICompleteInformationState next(IAction a) {
        DelayedActionPercept p = revealOpponentsDelayedAction();

        InformationSet nextX = xInfoSet, nextO = oInfoSet;
        int nextPlayer;
        if (actingPlayer == PLAYER_X) {
            nextPlayer = PLAYER_O;
            nextX = (InformationSet) xInfoSet.next(a).applyPercept(p);
        } else {
            nextPlayer = PLAYER_X;
            nextO = (InformationSet) oInfoSet.next(a).applyPercept(p);
        }
        return new CompleteInformationState(nextX, nextO, nextPlayer);
    }

    private DelayedActionPercept revealOpponentsDelayedAction() {
        int oppId = PlayerHelpers.getOpponentId(actingPlayer);
        MarkFieldAction opponentsDelayedAction = PlayerHelpers.selectByPlayerId(oppId, xInfoSet, oInfoSet).getDelayedAction();
        return new DelayedActionPercept(actingPlayer, opponentsDelayedAction);
    }

    @Override
    public Iterable<IPercept> getPercepts(IAction a) {
        return Collections.singleton(revealOpponentsDelayedAction());
    }

    private InformationSet getActingPlayerInfoSet() {
        if (actingPlayer == PLAYER_X) return xInfoSet;
        return oInfoSet;
    }

    private void detectTerminal() {
        if (terminal) return;
        boolean xWon = xInfoSet.hasPlayerWon();
        boolean oWon = oInfoSet.hasPlayerWon();
        terminal = !xInfoSet.hasLegalActions() || !oInfoSet.hasLegalActions() || oWon || (xWon && actingPlayer == PLAYER_X);
        if (!terminal) return;
        if (xWon == oWon) {
            xPayoff = 0;
        } else if (xWon) {
            xPayoff = 1;
        } else {
            xPayoff = -1;
        }
    }

    public InformationSet getInfoSetForPlayer(int role) {
        if (role == PLAYER_X) return xInfoSet;
        return oInfoSet;
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
        return actingPlayer == that.actingPlayer &&
                Objects.equals(xInfoSet, that.xInfoSet) &&
                Objects.equals(oInfoSet, that.oInfoSet);
    }

    @Override
    public int hashCode() {
        return Objects.hash(xInfoSet, oInfoSet, actingPlayer);
    }

    @Override
    public String toString() {
        return "LTTT{" +
                "x=" + xInfoSet +
                ", o=" + oInfoSet +
                ", terminal=" + terminal +
                ", actingPlayer=" + actingPlayer +
                ", xPayoff=" + xPayoff +
                '}';
    }
}

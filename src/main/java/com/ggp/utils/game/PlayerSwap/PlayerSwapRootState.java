package com.ggp.utils.game.PlayerSwap;

import com.ggp.*;
import com.ggp.utils.UniformRandomNode;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class PlayerSwapRootState implements ICompleteInformationState {
    private static final long serialVersionUID = 1L;
    private final ICompleteInformationState gameRoot;

    public PlayerSwapRootState(ICompleteInformationState gameRoot) {
        this.gameRoot = gameRoot;
    }

    @Override
    public boolean isTerminal() {
        return false;
    }

    @Override
    public int getActingPlayerId() {
        return 0;
    }

    @Override
    public double getPayoff(int player) {
        return 0;
    }

    @Override
    public List<IAction> getLegalActions() {
        return Arrays.asList(PlayerSwapAction.YES, PlayerSwapAction.NO);
    }

    @Override
    public IInformationSet getInfoSetForPlayer(int player) {
        return new PlayerSwapRootIs(gameRoot, player);
    }

    @Override
    public boolean isLegal(IAction a) {
        return a != null && a.getClass() == PlayerSwapAction.class;
    }

    @Override
    public ICompleteInformationState next(IAction a) {
        if (((PlayerSwapAction)a).isDoSwap()) return new PlayerSwapCISWrapper(gameRoot);
        return gameRoot;
    }

    @Override
    public Iterable<IPercept> getPercepts(IAction a) {
        boolean isSwapped = ((PlayerSwapAction)a).isDoSwap();
        return Arrays.asList(new PlayerSwapPercept(1, isSwapped), new PlayerSwapPercept(2, isSwapped));
    }

    @Override
    public IRandomNode getRandomNode() {
        return new UniformRandomNode(getLegalActions());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PlayerSwapRootState that = (PlayerSwapRootState) o;
        return Objects.equals(gameRoot, that.gameRoot);
    }

    @Override
    public int hashCode() {
        return Objects.hash(gameRoot);
    }

    @Override
    public String toString() {
        return "PlayerSwapRootState{" +
                    gameRoot +
                '}';
    }
}

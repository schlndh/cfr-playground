package com.ggp.utils.game.PlayerSwap;

import com.ggp.ICompleteInformationState;
import com.ggp.IGameDescription;

import java.util.Objects;

/**
 * Transforms the game to swap players at the beginning with 50% chance, which makes the game perfectly balanced (game-value is 0).
 */
public class PlayerSwapGameDescription implements IGameDescription {
    private static final long serialVersionUID = 1L;
    private final IGameDescription gameDesc;

    public PlayerSwapGameDescription(IGameDescription gameDesc) {
        if (gameDesc == null) {
            throw new IllegalArgumentException("Inner game description can't be null!");
        }
        this.gameDesc = gameDesc;
    }

    @Override
    public ICompleteInformationState getInitialState() {
        return new PlayerSwapRootState(gameDesc.getInitialState());
    }

    @Override
    public String toString() {
        return "PlayerSwap{" +
                    gameDesc.getConfigString() +
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
        PlayerSwapGameDescription that = (PlayerSwapGameDescription) o;
        return Objects.equals(gameDesc, that.gameDesc);
    }

    @Override
    public int hashCode() {
        return Objects.hash(gameDesc);
    }
}

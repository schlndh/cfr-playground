package com.ggp.games.RockPaperScissors;

import com.ggp.*;

import java.util.Objects;

public class GameDescription implements IGameDescription {
    private static final long serialVersionUID = 1L;
    private CompleteInformationState initialState;

    /**
     * @param size size of the game (only odd number are possible so that each choice covers and is covered by the same no of other choices)
     */
    public GameDescription(int size) {
        if (size % 2 == 0) throw new IllegalArgumentException("size must be odd");
        InformationSet p1 = new InformationSet(1, null, size),
                p2 = new InformationSet(2, null, size);
        initialState = new CompleteInformationState(p1, p2);
    }

    @Override
    public ICompleteInformationState getInitialState() {
        return initialState;
    }

    public ICompleteInformationStateFactory getCISFactory() {
        return new ICompleteInformationStateFactory() {
            private boolean isCompatible(IInformationSet player1, IInformationSet player2, int actingPlayer) {
                if (player1 == null || player2 == null || player1.getClass() != InformationSet.class
                        || player2.getClass() != InformationSet.class) return false;
                InformationSet is1 = (InformationSet) player1, is2 = (InformationSet) player2;
                if (is1.getOwnerId() != 1 || is2.getOwnerId() !=2 || is1.getSize() != is2.getSize()) return false;
                if (actingPlayer == 1 && is1.getChosenAction() != null) return false;
                if (actingPlayer == 2 && is2.getChosenAction() != null) return false;
                 return true;
            }

            @Override
            public ICompleteInformationState make(IInformationSet player1, IInformationSet player2, int actingPlayer) {
                if (!isCompatible(player1, player2, actingPlayer)) return null;
                return new CompleteInformationState((InformationSet) player1, (InformationSet) player2);
            }
        };
    }

    public ExploitabilityEstimator getExploitabilityEstimator() {
        return new ExploitabilityEstimator(initialState);
    }

    @Override
    public String toString() {
        return "RockPaperScissors{" +
                    initialState.getSize() +
                "}";
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
        return Objects.equals(initialState, that.initialState);
    }

    @Override
    public int hashCode() {
        return Objects.hash(initialState);
    }
}

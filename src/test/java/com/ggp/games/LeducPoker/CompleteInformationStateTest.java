package com.ggp.games.LeducPoker;

import com.ggp.games.LeducPoker.actions.CallAction;
import com.ggp.games.LeducPoker.actions.DealCardAction;
import com.ggp.games.LeducPoker.actions.FoldAction;
import com.ggp.games.LeducPoker.actions.RaiseAction;
import com.ggp.utils.GameUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CompleteInformationStateTest {
    @Test
    void testFold() {
        CompleteInformationState initialState = (CompleteInformationState) new GameDescription(7).getInitialState();
        CompleteInformationState round1Fold1 = (CompleteInformationState) GameUtils.applyActionSequnce(initialState,
                new DealCardAction(0, 1),
                new DealCardAction(1, 2),
                FoldAction.instance
        );
        assertEquals(-1d, round1Fold1.getPayoff(1));

        CompleteInformationState round2Winning1Fold1 = (CompleteInformationState) GameUtils.applyActionSequnce(initialState,
                new DealCardAction(0, 1),
                new DealCardAction(1, 2),
                CallAction.instance,
                CallAction.instance,
                new DealCardAction(0),
                FoldAction.instance
        );
        assertEquals(-1d, round2Winning1Fold1.getPayoff(1));

        CompleteInformationState round2Winning1Fold2 = (CompleteInformationState) GameUtils.applyActionSequnce(initialState,
                new DealCardAction(0, 1),
                new DealCardAction(1, 2),
                CallAction.instance,
                CallAction.instance,
                new DealCardAction(0),
                CallAction.instance,
                FoldAction.instance
        );
        assertEquals(1d, round2Winning1Fold2.getPayoff(1));

        CompleteInformationState round2Winning2Fold1 = (CompleteInformationState) GameUtils.applyActionSequnce(initialState,
                new DealCardAction(0, 1),
                new DealCardAction(1, 2),
                CallAction.instance,
                CallAction.instance,
                new DealCardAction(1),
                FoldAction.instance
        );
        assertEquals(-1d, round2Winning2Fold1.getPayoff(1));

        CompleteInformationState round2Winning2Fold2 = (CompleteInformationState) GameUtils.applyActionSequnce(initialState,
                new DealCardAction(0, 1),
                new DealCardAction(1, 2),
                CallAction.instance,
                CallAction.instance,
                new DealCardAction(1),
                CallAction.instance,
                FoldAction.instance
        );
        assertEquals(1d, round2Winning2Fold2.getPayoff(1));
    }

    @Test
    void testMultipleBets() {
        CompleteInformationState initialState = (CompleteInformationState) new GameDescription(50, 50, 3, 3).getInitialState();
        CompleteInformationState round1Fold1 = (CompleteInformationState) GameUtils.applyActionSequnce(initialState,
                new DealCardAction(0, 1),
                new DealCardAction(1, 2),
                CallAction.instance,
                RaiseAction.instance,
                RaiseAction.instance,
                RaiseAction.instance,
                CallAction.instance,
                new DealCardAction(2),
                FoldAction.instance
        );
        assertEquals(-7d, round1Fold1.getPayoff(1));
    }
}
package com.ggp.games.LatentTicTacToe;

import com.ggp.ICompleteInformationState;
import com.ggp.utils.GameRepository;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CompleteInformationStateTest {

    @Test
    void testWin1() {
        ICompleteInformationState state = GameRepository.latentTTT().getInitialState();
        state = state.next(new MarkFieldAction(0,0));
        state = state.next(new MarkFieldAction(0,0));
        state = state.next(new MarkFieldAction(0,1));
        state = state.next(new MarkFieldAction(2,2));
        state = state.next(new MarkFieldAction(0,2));
        assertFalse(state.isTerminal()); // give player 2 a chance as well
        state = state.next(new MarkFieldAction(1,2));
        assertTrue(state.isTerminal());
        assertEquals(1, state.getPayoff(1));
    }

    @Test
    void testWin2() {
        ICompleteInformationState state = GameRepository.latentTTT().getInitialState();
        state = state.next(new MarkFieldAction(0,0));
        state = state.next(new MarkFieldAction(2,2));
        state = state.next(new MarkFieldAction(2,2));
        state = state.next(new MarkFieldAction(2,1));
        state = state.next(new MarkFieldAction(0,2));
        state = state.next(new MarkFieldAction(2,0));
        assertTrue(state.isTerminal());
        assertEquals(-1, state.getPayoff(1));
    }

    @Test
    void testDraw() {
        ICompleteInformationState state = GameRepository.latentTTT().getInitialState();
        state = state.next(new MarkFieldAction(0,0));
        state = state.next(new MarkFieldAction(2,2));
        state = state.next(new MarkFieldAction(0,1));
        state = state.next(new MarkFieldAction(2,1));
        state = state.next(new MarkFieldAction(0,2));
        state = state.next(new MarkFieldAction(2,0));
        assertTrue(state.isTerminal());
        assertEquals(0, state.getPayoff(1));
    }
}
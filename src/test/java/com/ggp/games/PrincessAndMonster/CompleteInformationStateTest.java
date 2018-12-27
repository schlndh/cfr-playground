package com.ggp.games.PrincessAndMonster;

import com.ggp.ICompleteInformationState;
import com.ggp.games.PrincessAndMonster.graphs.Grid3x3;
import com.ggp.utils.GameRepository;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CompleteInformationStateTest {
    @Test
    void testRun() {
        CompleteInformationState initialState = (CompleteInformationState) GameRepository.princessAndMonster(3).getInitialState();
        ICompleteInformationState state = initialState;
        state = state.next(Grid3x3.getPosition(1,0));
        state = state.next(Grid3x3.getPosition(1,2));
        state = state.next(Grid3x3.getPosition(1,1));
        state = state.next(Grid3x3.getPosition(1,1));
        // princess lost after 2 turns
        assertTrue(state.isTerminal());
        assertEquals(-2, state.getPayoff(1));
        assertEquals(2, state.getPayoff(2));

        state = initialState;
        state = state.next(Grid3x3.getPosition(1,0));
        state = state.next(Grid3x3.getPosition(1,2));
        state = state.next(Grid3x3.getPosition(2,0));
        state = state.next(Grid3x3.getPosition(1,1));
        state = state.next(Grid3x3.getPosition(2,1));
        state = state.next(Grid3x3.getPosition(1,0));
        // princess won
        assertTrue(state.isTerminal());
        assertEquals(6, state.getPayoff(1));
        assertEquals(-6, state.getPayoff(2));
    }
}
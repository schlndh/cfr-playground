package com.ggp;

import com.ggp.games.LeducPoker.GameDescription;
import com.ggp.games.LeducPoker.actions.CallAction;
import com.ggp.games.LeducPoker.actions.DealCardAction;
import com.ggp.games.LeducPoker.actions.FoldAction;
import com.ggp.games.LeducPoker.actions.RaiseAction;
import com.ggp.players.random.RandomPlayer;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GameManagerTest {

    @Test
    void testForcedRun() {
        GameManager gameManager = new GameManager(new RandomPlayer.Factory(), new RandomPlayer.Factory(), new GameDescription(7));
        ArrayList<IAction> actions = new ArrayList<>();
        List<IAction> forcedActions = Arrays.asList(
                new DealCardAction(0, 1),
                new DealCardAction(1, 2),
                CallAction.instance,
                RaiseAction.instance,
                CallAction.instance,
                new DealCardAction(2),
                CallAction.instance,
                RaiseAction.instance,
                FoldAction.instance
        );


        gameManager.registerGameListener(new IGameListener() {
            @Override
            public void playerInitStarted(int player) {
            }

            @Override
            public void playerInitFinished(int player) {
            }

            @Override
            public void gameStart(IPlayer player1, IPlayer player2) { }

            @Override
            public void gameEnd(int payoff1, int payoff2) { }

            @Override
            public void stateReached(ICompleteInformationState s) { }

            @Override
            public void actionSelected(ICompleteInformationState s, IAction a) {
                actions.add(a);
            }
        });
        gameManager.run(1, 1, forcedActions.iterator());
        assertEquals(forcedActions, actions);
    }
}
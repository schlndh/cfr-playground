package com.ggp.player_evaluators;

import com.ggp.IGameDescription;
import com.ggp.players.deepstack.ISubgameResolver;

import java.util.List;

public interface IPlayerEvaluator {
    interface IFactory {
        IPlayerEvaluator create(int initMs, List<Integer> logPointsMs);
        String getConfigString();
    }
    /**
     * Evaluates player's performance in given game.
     * @param gameDesc
     * @param playerFactory
     * @param quiet
     * @return list of evaluator entries ordered by time ASC.
     */
    List<EvaluatorEntry> evaluate(IGameDescription gameDesc, IEvaluablePlayer.IFactory playerFactory, boolean quiet);
}

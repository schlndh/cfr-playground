package com.ggp.player_evaluators;

import com.ggp.IGameDescription;
import com.ggp.utils.time.TimeLimit;

import java.io.IOException;
import java.util.List;

public interface IPlayerEvaluator {
    interface IFactory {
        IPlayerEvaluator create(int initMs, List<Integer> logPointsMs);
        String getConfigString();
        IPlayerEvaluationSaver createSaver(String path, int initMs, String postfix) throws IOException;
    }
    /**
     * Evaluates player's performance in given game.
     * @param gameDesc
     * @param playerFactory
     * @param quiet
     * @param evaluationTimeLimit - max. evaluation time
     * @return list of evaluator entries ordered by time ASC.
     */
    List<EvaluatorEntry> evaluate(IGameDescription gameDesc, IEvaluablePlayer.IFactory playerFactory, boolean quiet, TimeLimit evaluationTimeLimit);
}

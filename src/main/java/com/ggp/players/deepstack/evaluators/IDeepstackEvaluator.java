package com.ggp.players.deepstack.evaluators;

import com.ggp.IGameDescription;
import com.ggp.players.deepstack.ISubgameResolver;

import java.util.List;

public interface IDeepstackEvaluator {
    interface IFactory {
        IDeepstackEvaluator create(int initMs, List<Integer> logPointsMs);
    }
    /**
     * Evaluates given Deepstack configuration in given game.
     * @param gameDesc
     * @param subgameResolverFactory
     * @param quiet
     * @return ASC ordered list of entries containing times and corresponding normalized aggregated strategies.
     */
    List<EvaluatorEntry> evaluate(IGameDescription gameDesc, ISubgameResolver.Factory subgameResolverFactory, boolean quiet);

    String getConfigString();
}

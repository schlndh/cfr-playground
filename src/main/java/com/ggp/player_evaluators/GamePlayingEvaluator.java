package com.ggp.player_evaluators;

import com.ggp.GameManager;
import com.ggp.IGameDescription;
import com.ggp.IPlayerFactory;
import com.ggp.player_evaluators.listeners.StrategyAggregatorListener;
import com.ggp.utils.exploitability.ExploitabilityUtils;
import com.ggp.utils.strategy.Strategy;
import com.ggp.players.random.RandomPlayer;
import com.ggp.utils.strategy.NormalizingStrategyWrapper;

import java.util.List;

/**
 * Evaluates Deepstack configuration by playing number of games against random opponent, while aggregating computed strategies
 * at encountered decision points.
 */
public class GamePlayingEvaluator implements IPlayerEvaluator {
    public static class Factory implements IFactory {
        private int gameCount;

        public Factory(int gameCount) {
            this.gameCount = gameCount;
        }

        @Override
        public IPlayerEvaluator create(int initMs, List<Integer> logPointsMs) {
            return new GamePlayingEvaluator(initMs, logPointsMs, gameCount);
        }
    }

    private int initMs;
    private int timeoutMs;
    StrategyAggregatorListener stratAggregator;
    private int gameCount;

    /**
     * Constructor
     * @param initMs timeout for deepstack initialization
     * @param logPointsMs ASC ordered list of times when strategies should be aggregated
     * @param gameCount number of games to play
     */
    public GamePlayingEvaluator(int initMs, List<Integer> logPointsMs, int gameCount) {
        this.initMs = initMs;
        this.timeoutMs = logPointsMs.get(logPointsMs.size() - 1);
        this.stratAggregator = new StrategyAggregatorListener(logPointsMs);
        this.gameCount = gameCount;
    }

    @Override
    public List<EvaluatorEntry> evaluate(IGameDescription gameDesc, IEvaluablePlayer.IFactory playerFactory, boolean quiet) {
        IPlayerFactory random = new RandomPlayer.Factory();
        long lastVisitedStates[] = new long[stratAggregator.getEntries().size()];
        playerFactory.registerResolvingListener(stratAggregator);
        for (int i = 0; i < gameCount; ++i) {
            IPlayerFactory pl1 = playerFactory, pl2 = random;
            if (i % 2 == 1) {
                pl1 = random;
                pl2 = playerFactory;
            }
            stratAggregator.reinit();
            GameManager manager = new GameManager(pl1, pl2, gameDesc);
            manager.run(initMs, timeoutMs);
            List<EvaluatorEntry> entries = stratAggregator.getEntries();
            for (int j = 0; j < entries.size(); ++j) {
                EvaluatorEntry e = entries.get(j);
                e.addPathStates(e.getAvgVisitedStates() - lastVisitedStates[j], 1);
                lastVisitedStates[j] = e.getAvgVisitedStates();
            }
            Strategy strat = entries.get(entries.size() - 1).getAggregatedStrat();
            double exp = ExploitabilityUtils.computeExploitability(new NormalizingStrategyWrapper(strat), gameDesc);
            if (!quiet) System.out.println(String.format("Game %d: defined IS %d, last strategy exploitability %f", i, strat.countDefinedInformationSets(), exp));
        }

        for (EvaluatorEntry entry: stratAggregator.getEntries()) {
            entry.getAggregatedStrat().normalize();
            entry.setVisitedStatesNorm(gameCount);
        }
        return stratAggregator.getEntries();
    }

    @Override
    public String getConfigString() {
        return "GamePlayingEvaluator{" +
                    gameCount +
                '}';
    }
}
package com.ggp.player_evaluators;

import com.ggp.*;
import com.ggp.player_evaluators.listeners.StrategyAggregatorListener;
import com.ggp.utils.exploitability.ExploitabilityUtils;
import com.ggp.utils.strategy.Strategy;
import com.ggp.players.random.RandomPlayer;
import com.ggp.utils.strategy.NormalizingStrategyWrapper;

import java.util.HashSet;
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

        @Override
        public String getConfigString() {
            return "GamePlayingEvaluator{" +
                    gameCount +
                    '}';
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

    private void visit(HashSet<IInformationSet> infoSets, ICompleteInformationState s) {
        if (s.isTerminal()) {
            return;
        }
        if (s.isRandomNode()) {
            for (IAction a: s.getLegalActions()) {
                visit(infoSets, s.next(a));
            }
        } else {
            infoSets.add(s.getInfoSetForActingPlayer());
            for (IAction a: s.getLegalActions()) {
                visit(infoSets, s.next(a));
            }
        }
    }

    private int countInfoSets(IGameDescription gameDesc) {
        HashSet<IInformationSet> infoSets = new HashSet<>();
        visit(infoSets, gameDesc.getInitialState());
        return infoSets.size();
    }

    @Override
    public List<EvaluatorEntry> evaluate(IGameDescription gameDesc, IEvaluablePlayer.IFactory playerFactory, boolean quiet) {
        IPlayerFactory random = new RandomPlayer.Factory();
        long lastVisitedStates[] = new long[stratAggregator.getEntries().size()];
        playerFactory.registerResolvingListener(stratAggregator);
        final int evalEach = gameCount/Math.min(gameCount, 10);
        int evals = 0;
        final int totalInfoSets = countInfoSets(gameDesc);
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
            if (!quiet) {
                if ((i+1) == evalEach*(evals + 1)) {
                    double exp = ExploitabilityUtils.computeExploitability(new NormalizingStrategyWrapper(strat), gameDesc);
                    System.out.println(String.format("\rGame %d: defined IS %d/%d, last strategy exploitability %f", i+1, strat.size(), totalInfoSets, exp));
                    evals++;
                } else {
                    System.out.print(String.format("\rGame %d: defined IS %d/%d", i+1, strat.size(), totalInfoSets));
                }
            }

        }

        for (EvaluatorEntry entry: stratAggregator.getEntries()) {
            entry.getAggregatedStrat().normalize();
            entry.setVisitedStatesNorm(gameCount);
        }
        return stratAggregator.getEntries();
    }
}

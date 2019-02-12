package com.ggp.cli;

import com.ggp.GameManager;
import com.ggp.IGameDescription;
import com.ggp.IPlayerFactory;
import com.ggp.parsers.ParseUtils;
import com.ggp.parsers.exceptions.ConfigAssemblyException;
import com.ggp.player_evaluators.IEvaluablePlayer;
import com.ggp.player_evaluators.EvaluatorEntry;
import com.ggp.player_evaluators.IPlayerEvaluator;
import com.ggp.utils.exploitability.ExploitabilityUtils;
import com.ggp.utils.strategy.Strategy;
import picocli.CommandLine;

import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.util.Collections;
import java.util.Date;

@CommandLine.Command(name = "evaluate",
        mixinStandardHelpOptions = true,
        description = "Runs online evaluation of given player",
        optionListHeading = "%nOptions:%n",
        sortOptions = false
)
public class EvaluateCommand implements Runnable {
    @CommandLine.ParentCommand
    private MainCommand mainCommand;

    @CommandLine.Option(names={"-g", "--game"}, description="Game to be played", required=true)
    private String game;

    @CommandLine.Option(names={"-p", "--player"}, description="Player to evaluate")
    private String player;

    @CommandLine.Option(names={"-e", "--evaluator"}, description="Evaluator")
    private String evaluator;

    @CommandLine.Option(names={"-i", "--init"}, description="Init time (ms)", required=true)
    private int init;

    @CommandLine.Option(names={"-t", "--time-limit"}, description="Time per move (ms)", required=true)
    private int timeLimit;

    @CommandLine.Option(names={"-f", "--eval-freq"}, description="Evaluation frequency (ms)", defaultValue="-1")
    private int evalFreq;

    @CommandLine.Option(names={"-c", "--count"}, description="How many times to repeat the evaluation", defaultValue="1")
    private int count;

    @CommandLine.Option(names={"-d", "--dry-run"}, description="Dry run - doesn't save output")
    private boolean dryRun;

    @CommandLine.Option(names={"-q", "--quiet"}, description="Quiet mode - doesn't print output")
    private boolean quiet;

    @CommandLine.Option(names={"--is-targeting"}, description="Use IS targeting")
    private boolean useISTargeting;

    @CommandLine.Option(names={"--res-dir"}, description="Results directory", defaultValue="player-results")
    private String resultsDirectory;

    @CommandLine.Option(names={"--save-strategy"}, description="Save computed strategy", defaultValue = "false")
    private boolean saveStrategy;

    @CommandLine.Option(names={"--res-postfix"}, description="Postfix for result files", defaultValue="0")
    private String resultPostfix;

    @CommandLine.Option(names={"--skip-warmup"}, description="Skip warm-up")
    private boolean skipWarmup;

    private String getDateKey() {
        return String.format("%1$tY%1$tm%1$td-%1$tH%1$tM%1$tS", new Date());
    }

    private void warmup(IGameDescription gameDesc, IPlayerFactory plFactory) {
        if (skipWarmup) return;
        if (!quiet) System.out.println("Warming up...");
        GameManager manager = new GameManager(plFactory, plFactory, gameDesc);
        manager.run(4000, 1000);
        if (!quiet) System.out.println("Warm-up complete.");
    }

    @Override
    public void run() {
        if (evalFreq <= 0) {
            evalFreq = timeLimit;
        }
        IGameDescription gameDesc = null;
        try {
            gameDesc = mainCommand.getConfigurableFactory().create(IGameDescription.class, ParseUtils.parseConfigExpression(game));
        } catch (ConfigAssemblyException e) { }

        if (gameDesc == null) {
            throw new CommandLine.ParameterException(new CommandLine(this), "Failed to setup game '" + game + "'.", null, game);
        }

        IEvaluablePlayer.IFactory usedPlayerFactory = null;
        try {
            usedPlayerFactory = (IEvaluablePlayer.IFactory) mainCommand.getConfigurableFactory().create(IPlayerFactory.class, ParseUtils.parseConfigExpression(player));
        } catch (ClassCastException e) {
            throw new CommandLine.ParameterException(new CommandLine(this), "Player " + player + " doesn't support evaluation.", null, player);
        } catch (ConfigAssemblyException e) { }

        if (usedPlayerFactory == null) {
            throw new CommandLine.ParameterException(new CommandLine(this), "Failed to setup player '" + player + "'.", null, player);
        }

        IPlayerEvaluator.IFactory usedEvaluatorFactory = null;
        try {
            usedEvaluatorFactory = mainCommand.getConfigurableFactory().create(IPlayerEvaluator.IFactory.class, ParseUtils.parseConfigExpression(evaluator));
        } catch (ConfigAssemblyException e) {}

        if (usedEvaluatorFactory == null) {
            throw new CommandLine.ParameterException(new CommandLine(this), "Failed to setup evalautor '" + evaluator + "'.", null, evaluator);
        }

        if (!quiet) System.out.println("Exploitability estimate for uniform strategy: " + ExploitabilityUtils.computeExploitability(new Strategy(), gameDesc));

        String gameDir = resultsDirectory + "/" + gameDesc.getConfigString();
        String solverDir =  gameDir + "/" + usedPlayerFactory.getConfigString();
        if (!dryRun)
            new File(solverDir).mkdirs();

        if (!quiet) System.out.println("Evaluating " + usedPlayerFactory.getConfigString() + " using " + usedEvaluatorFactory.getConfigString());
        warmup(gameDesc, usedPlayerFactory);

        Strategy bestStrategy = null;
        double bestStrategyExp = Double.MAX_VALUE;

        for (int repetition = 0; repetition < count; ++repetition) {
            if (!quiet && count > 1) {
                System.out.println(String.format("Evaluation %d/%d:", repetition + 1, count));
            }
            long lastEntryStates = 0;
            double lastTime = 0;

            int logTimeMs = Math.min(evalFreq, timeLimit);
            do {
                IPlayerEvaluator evaluator = usedEvaluatorFactory.create(init, Collections.singletonList(logTimeMs));
                EvaluatorEntry entry = evaluator.evaluate(gameDesc, usedPlayerFactory, quiet).get(0);
                double exp = ExploitabilityUtils.computeExploitability(entry.getAggregatedStrat(), gameDesc);
                if (exp < bestStrategyExp) {
                    bestStrategy = entry.getAggregatedStrat();
                    bestStrategyExp = exp;
                }
                if (!quiet) {
                    System.out.println(String.format("(%5d ms, %12d total states, %8d avg. path states) -> %.4f exp | %.4g states/s",
                            (int) entry.getEntryTimeMs(), entry.getAvgVisitedStates(), entry.getPathStatesAvg(),exp, 1000*(entry.getAvgVisitedStates() - lastEntryStates)/(entry.getEntryTimeMs() - lastTime)));
                }
                lastEntryStates = entry.getAvgVisitedStates();
                lastTime = entry.getEntryTimeMs();

                logTimeMs += evalFreq;
            } while (logTimeMs <= timeLimit);
        }


        if (saveStrategy && bestStrategy != null) {
            if (!quiet) {
                System.out.println(String.format("Best exploitability estimate: %f", bestStrategyExp));
            }

            bestStrategy.normalize();
            try {
                String resFileName = String.format("%1$s/%2$s-solution-%3$.4f-%4$s.strat", solverDir, getDateKey(),  bestStrategyExp, resultPostfix);
                if (!quiet) System.out.println("Saving to: " + resFileName);
                FileOutputStream fileOutputStream = new FileOutputStream(resFileName);
                ObjectOutputStream objectOutputStream
                        = new ObjectOutputStream(fileOutputStream);
                objectOutputStream.writeObject(bestStrategy);
                objectOutputStream.flush();
                objectOutputStream.close();
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }
}

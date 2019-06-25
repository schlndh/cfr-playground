package com.ggp.cli;

import com.ggp.IGameDescription;
import com.ggp.IPlayerFactory;
import com.ggp.player_evaluators.IEvaluablePlayer;
import com.ggp.player_evaluators.EvaluatorEntry;
import com.ggp.player_evaluators.IPlayerEvaluationSaver;
import com.ggp.player_evaluators.IPlayerEvaluator;
import com.ggp.utils.exploitability.ExploitabilityUtils;
import com.ggp.utils.strategy.NormalizingStrategyWrapper;
import com.ggp.utils.strategy.Strategy;
import com.ggp.utils.time.StopWatch;
import com.ggp.utils.time.TimeLimit;
import picocli.CommandLine;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Arrays;
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

    @CommandLine.Option(names={"-g", "--game"}, description="Game to be played (IGameDescription)", required=true)
    private IGameDescription game;

    @CommandLine.Option(names={"-p", "--player"}, description="Player to evaluate (IPlayerFactory)")
    private IPlayerFactory player;

    @CommandLine.Option(names={"-e", "--evaluator"}, description="Evaluator (IPlayerEvaluator.IFactory)")
    private IPlayerEvaluator.IFactory evaluator;

    @CommandLine.Option(names={"-i", "--init"}, description="Init time (ms)", required=true)
    private int init;

    @CommandLine.Option(names={"-t", "--time-limits"}, description="Time limits per move (ms)", required=true, arity="1..*")
    private int[] timeLimits;

    @CommandLine.Option(names={"-c", "--count"}, description="How many times to repeat the evaluation", defaultValue="1")
    private int count;

    @CommandLine.Option(names={"-d", "--dry-run"}, description="Dry run - doesn't save output")
    private boolean dryRun;

    @CommandLine.Option(names={"-q", "--quiet"}, description="Quiet mode - doesn't print output")
    private boolean quiet;

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

    private Strategy bestStrategy = null;
    private double bestStrategyExp = Double.MAX_VALUE;

    private void warmup(IPlayerEvaluator.IFactory usedEvaluatorFactory, IEvaluablePlayer.IFactory usedPlayerFactory, IGameDescription gameDesc) {
        if (skipWarmup) return;
        if (!quiet) System.out.println("Warming up...");
        StopWatch warmupTimer = new StopWatch();
        warmupTimer.start();
        do {
            runEvaluator(usedEvaluatorFactory, usedPlayerFactory, gameDesc, null, true,2000, new int[]{250}, new TimeLimit(10000));
        } while (warmupTimer.getLiveDurationMs() < 30000);
        if (!quiet) System.out.println(String.format("Warm-up complete in %dms.", warmupTimer.getLiveDurationMs()));
    }

    private void runEvaluator(IPlayerEvaluator.IFactory usedEvaluatorFactory, IEvaluablePlayer.IFactory usedPlayerFactory,
                              IGameDescription gameDesc, IPlayerEvaluationSaver saver, boolean quiet, int initMs,
                              int[] timeLimitsMs, TimeLimit evaluationTimeLimit) {
        if (evaluationTimeLimit != null) evaluationTimeLimit.start();
        long lastEntryStates = 0;
        double lastTime = 0;
        try {
            for (int logTimeMs: timeLimitsMs) {
                IPlayerEvaluator evaluator = usedEvaluatorFactory.create(initMs, Collections.singletonList(logTimeMs));
                EvaluatorEntry entry = evaluator.evaluate(gameDesc, usedPlayerFactory, quiet, evaluationTimeLimit).get(0);
                double exp = ExploitabilityUtils.computeExploitability(new NormalizingStrategyWrapper(entry.getAggregatedStrat()), gameDesc);
                double firstActExp = ExploitabilityUtils.computeExploitability(new NormalizingStrategyWrapper(entry.getFirstActionStrat()), gameDesc);
                if (saver != null) {
                    saver.add(entry, exp, firstActExp);
                }
                if (exp < bestStrategyExp) {
                    bestStrategy = entry.getAggregatedStrat();
                    bestStrategyExp = exp;
                }
                if (!quiet) {
                    System.out.println(String.format("(%5d ms, %12d total states, %8d avg. path states) -> (%10.4g exp, %10.4g first act exp) | %.4g states/s",
                            (int) entry.getEntryTimeMs(), entry.getAvgVisitedStates(), entry.getPathStatesAvg(),
                            exp, firstActExp, 1000*(entry.getAvgVisitedStates() - lastEntryStates)/(entry.getEntryTimeMs() - lastTime)));
                }
                lastEntryStates = entry.getAvgVisitedStates();
                lastTime = entry.getEntryTimeMs();
                if (evaluationTimeLimit != null && evaluationTimeLimit.isFinished()) break;
            }
        } catch(IOException e) {
            System.out.println(e.getMessage());
        }
    }

    @Override
    public void run() {
        if (game == null) {
            System.out.println("Game can't be null!");
            return;
        }

        if (player == null) {
            System.out.println("Player can't be null!");
            return;
        }

        if (evaluator == null) {
            System.out.println("Evaluator can't be null!");
            return;
        }

        IEvaluablePlayer.IFactory usedPlayerFactory;
        try {
            usedPlayerFactory = (IEvaluablePlayer.IFactory) player;
        } catch (ClassCastException e) {
            System.out.println("Player is not evaluable!");
            return;
        }

        if (!quiet) System.out.println("Exploitability estimate for uniform strategy: " + ExploitabilityUtils.computeExploitability(new Strategy(), game));

        String gameDir = resultsDirectory + "/" + game.getConfigString();
        String solverDir =  gameDir + "/" + usedPlayerFactory.getConfigString();
        if (!dryRun || saveStrategy)
            new File(solverDir).mkdirs();

        if (!quiet) {
            if (dryRun) {
                System.out.println(String.format("Evaluating %s using %s on %s.", usedPlayerFactory.getConfigString(),
                        evaluator.getConfigString(), game.getConfigString()));
            } else {
                System.out.println(String.format("Evaluating %s using %s on %s logged to %s.", usedPlayerFactory.getConfigString(),
                        evaluator.getConfigString(), game.getConfigString(), solverDir));
            }
        }
        warmup(evaluator, usedPlayerFactory, game);

        Strategy bestStrategy = null;
        double bestStrategyExp = Double.MAX_VALUE;

        Arrays.sort(timeLimits);

        for (int repetition = 0; repetition < count; ++repetition) {
            if (!quiet && count > 1) {
                System.out.println(String.format("Evaluation %d/%d:", repetition + 1, count));
            }
            IPlayerEvaluationSaver saver = null;
            if (!dryRun) {
                try {
                    saver = evaluator.createSaver(solverDir, init, resultPostfix);
                } catch (IOException e) {
                    System.out.println(e.getMessage());
                    continue;
                }
            }
            runEvaluator(evaluator, usedPlayerFactory, game, saver, quiet, init, timeLimits, null);
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

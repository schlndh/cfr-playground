package com.ggp.cli;

import com.ggp.*;
import com.ggp.parsers.ParseUtils;
import com.ggp.parsers.exceptions.ConfigAssemblyException;
import com.ggp.players.deepstack.trackers.IGameTraversalTracker;
import com.ggp.players.deepstack.trackers.SimpleTracker;
import com.ggp.utils.exploitability.ExploitabilityUtils;
import com.ggp.utils.strategy.Strategy;
import com.ggp.solvers.cfr.BaseCFRSolver;
import com.ggp.utils.strategy.NormalizingStrategyWrapper;
import com.ggp.utils.time.StopWatch;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import picocli.CommandLine;

import java.io.*;
import java.util.Date;

@CommandLine.Command(name = "solve",
        mixinStandardHelpOptions = true,
        description = "Solves the game",
        optionListHeading = "%nOptions:%n",
        sortOptions = false
)
public class SolveCommand implements Runnable {
    @CommandLine.ParentCommand
    private MainCommand mainCommand;

    @CommandLine.Option(names={"-g", "--game"}, description="game to be played", required=true)
    private String game;

    @CommandLine.Option(names={"-s", "--solver"}, description="CFR solver", required=true)
    private String solver;

    @CommandLine.Option(names={"-t", "--time-limit"}, description="Time limit (s)", required=true)
    private long timeLimit;

    @CommandLine.Option(names={"-f", "--eval-freq"}, description="Evaluation frequency (ms)", defaultValue = "-1")
    private long evalFreq;

    @CommandLine.Option(names={"-c", "--count"}, description="How many times to repeat the evaluation", defaultValue="1")
    private int count;

    @CommandLine.Option(names={"-d", "--dry-run"}, description="Dry run - doesn't save output")
    private boolean dryRun;

    @CommandLine.Option(names={"-q", "--quiet"}, description="Quiet mode - doesn't print output")
    private boolean quiet;

    @CommandLine.Option(names={"--skip-warmup"}, description="Skip warm-up")
    private boolean skipWarmup;

    @CommandLine.Option(names={"--res-dir"}, description="Results directory", defaultValue="results")
    private String resultsDirectory;

    @CommandLine.Option(names={"--save-strategy"}, description="Save computed strategy", defaultValue = "false")
    private boolean saveStrategy;

    @CommandLine.Option(names={"--res-postfix"}, description="Postfix for result files", defaultValue="0")
    private String resultPostfix;

    private String getDateKey() {
        return String.format("%1$tY%1$tm%1$td-%1$tH%1$tM%1$tS", new Date());
    }

    private String getCSVName() {
        return String.format("%d-%d-%s-%s.csv", timeLimit, evalFreq, getDateKey(), resultPostfix.replace("-", ""));
    }

    private void printUniformExp(IGameDescription gameDesc) {
        if (quiet) return;
        StopWatch expTimer = new StopWatch();
        expTimer.start();
        double exp = ExploitabilityUtils.computeExploitability(new Strategy(), gameDesc);
        expTimer.stop();
        System.out.println("Exploitability estimate for uniform strategy: " + exp + " in " + expTimer.getDurationMs() + " ms");
    }

    private void warmup(IGameDescription gameDesc, BaseCFRSolver.Factory solverFactory) {
        if (skipWarmup) return;
        if (!quiet) System.out.println("Warming up ...");

        for (int i = 0; i < 5; ++i) {
            runSolver(solverFactory, gameDesc, 1000, 5, true, -1, new StringWriter());
        }

        if (!quiet) System.out.println("Warm-up complete.");
    }

    private Strategy runSolver(BaseCFRSolver.Factory usedSolverFactory, IGameDescription gameDesc, long freqMs, long evalEntries, boolean quiet, double returnStratThreshold, Writer fileOutput) {
        try {
            CSVPrinter csvOut = new CSVPrinter(fileOutput,
                    CSVFormat.EXCEL.withHeader("intended_time", "time", "iterations", "states", "exp", "avg_regret"));

            BaseCFRSolver cfrSolver = usedSolverFactory.create(null);
            IGameTraversalTracker tracker = SimpleTracker.createRoot(gameDesc.getInitialState());
            int entryIdx = 0;
            final long evaluateAfterMs = freqMs;
            StopWatch timer = new StopWatch(), evaluationTimer = new StopWatch();
            timer.start();
            evaluationTimer.start();
            long iter = 0, lastEvalIters = 0;
            double strategyExp = 0;
            while (entryIdx < evalEntries) {
                do {
                    iter++;
                    cfrSolver.runIteration(tracker);
                } while (timer.getLiveDurationMs() < (entryIdx+1)*evaluateAfterMs);

                timer.stop();
                evaluationTimer.stop();
                long visitedStates = cfrSolver.getVisitedStates();
                double exp = ExploitabilityUtils.computeExploitability(new NormalizingStrategyWrapper(cfrSolver.getCumulativeStrat()), gameDesc);
                strategyExp = exp;
                double avgRegret = cfrSolver.getTotalRegret() / iter;
                csvOut.printRecord((entryIdx+1) * evaluateAfterMs ,timer.getDurationMs(), iter, visitedStates, exp, avgRegret);
                csvOut.flush();

                String status = String.format("(%8d ms, %10d iterations, %12d states) -> (%.4f exp, %.4f avg. regret) | %.4g iters/s",
                        timer.getDurationMs(), iter, visitedStates, exp, avgRegret, 1000*(iter - lastEvalIters)/((double)evaluationTimer.getDurationMs()));
                if (!quiet) {
                    System.out.println(status);
                }
                while (timer.getDurationMs() >= (entryIdx+1)*evaluateAfterMs) entryIdx++;
                lastEvalIters = iter;
                evaluationTimer.reset();
                timer.start();
            }
            if (strategyExp < returnStratThreshold) {
                return cfrSolver.getFinalCumulativeStrat();
            }
            csvOut.close();
            return null;
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public void run() {
        if (evalFreq <= 0) {
            evalFreq = timeLimit*1000;
        }
        IGameDescription gameDesc = null;
        try {
            gameDesc = mainCommand.getConfigurableFactory().create(IGameDescription.class, ParseUtils.parseConfigExpression(game));
        } catch (ConfigAssemblyException e) { }

        if (gameDesc == null) {
            throw new CommandLine.ParameterException(new CommandLine(this), "Failed to setup game '" + game + "'.", null, game);
        }

        BaseCFRSolver.Factory usedSolverFactory = null;
        try {
            usedSolverFactory = mainCommand.getConfigurableFactory().create(BaseCFRSolver.Factory.class, ParseUtils.parseConfigExpression(solver));
        } catch (ConfigAssemblyException e) { }

        if (usedSolverFactory == null) {
            throw new CommandLine.ParameterException(new CommandLine(this), "Failed to setup solver '" + solver + "'.", null, solver);
        }
        String gameDir = resultsDirectory + "/" + gameDesc.getConfigString();
        String solverDir =  gameDir + "/" + usedSolverFactory.getConfigString();
        if (!dryRun)
            new File(solverDir).mkdirs();
        if (!quiet) {
            if (dryRun) {
                System.out.println(String.format("Solving %s with %s", gameDesc.getConfigString(), usedSolverFactory.getConfigString()));
            } else {
                System.out.println(String.format("Solving %s with %s logged to %s.", gameDesc.getConfigString(), usedSolverFactory.getConfigString(), solverDir));
            }
        }

        warmup(gameDesc, usedSolverFactory);
        printUniformExp(gameDesc);
        Strategy bestStrategy = null;
        double bestStrategyExp = saveStrategy ? Double.MAX_VALUE : -1;
        final int evalEntriesCount = (int)(timeLimit*1000/evalFreq);
        for (int i = 0; i < count; ++i) {
            String csvFileName = solverDir + "/" + getCSVName();
            try {
                Writer output = dryRun ? new StringWriter() : new FileWriter(csvFileName);
                Strategy newBestStrat = runSolver(usedSolverFactory, gameDesc, evalFreq, evalEntriesCount, quiet, bestStrategyExp, output);
                if (newBestStrat != null) {
                    bestStrategy = newBestStrat;
                    newBestStrat.normalize();
                    bestStrategyExp = ExploitabilityUtils.computeExploitability(newBestStrat, gameDesc);
                }
            } catch (IOException e) {
                continue;
            }
        }


        if (saveStrategy && bestStrategy != null) {
            if (!quiet) {
                System.out.println(String.format("Best exploitability estimate: %f", bestStrategyExp));
            }

            bestStrategy.normalize();
            try {
                String resFileName = String.format("%1$s/%2$s-solution-%3$.4f-.strat", solverDir, getDateKey(),  bestStrategyExp, resultPostfix);
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

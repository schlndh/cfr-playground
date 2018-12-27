package com.ggp.cli;

import com.ggp.*;
import com.ggp.parsers.ParseUtils;
import com.ggp.parsers.exceptions.ConfigAssemblyException;
import com.ggp.players.deepstack.trackers.IGameTraversalTracker;
import com.ggp.players.deepstack.trackers.SimpleTracker;
import com.ggp.utils.strategy.Strategy;
import com.ggp.solvers.cfr.BaseCFRSolver;
import com.ggp.utils.strategy.NormalizingStrategyWrapper;
import com.ggp.utils.time.StopWatch;
import com.ggp.utils.recall.ImperfectRecallExploitability;
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

    @CommandLine.Option(names={"-e", "--eval-freq"}, description="Evaluation frequency (ms)", required=true)
    private long evalFreq;

    @CommandLine.Option(names={"-c", "--count"}, description="How many times to repeat the evaluation", defaultValue="1")
    private int count;

    @CommandLine.Option(names={"--save-strategy"}, description="Save computed strategy", defaultValue = "false")
    private boolean saveStrategy;

    private String getDateKey() {
        return String.format("%1$tY%1$tm%1$td-%1$tH%1$tM%1$tS", new Date());
    }

    @Override
    public void run() {
        IGameDescription gameDesc = null;
        try {
            gameDesc = mainCommand.getConfigurableFactory().create(IGameDescription.class, ParseUtils.parseConfigExpression(game));
        } catch (ConfigAssemblyException e) { }

        if (gameDesc == null) {
            throw new CommandLine.ParameterException(new CommandLine(this), "Failed to setup game '" + game + "'.", null, game);
        }

        System.out.println("Exploitability estimate for uniform strategy: " + ImperfectRecallExploitability.computeExploitability(new Strategy(), gameDesc));
        BaseCFRSolver.Factory usedSolverFactory = null;
        try {
            usedSolverFactory = mainCommand.getConfigurableFactory().create(BaseCFRSolver.Factory.class, ParseUtils.parseConfigExpression(solver));
        } catch (ConfigAssemblyException e) { }

        if (usedSolverFactory == null) {
            throw new CommandLine.ParameterException(new CommandLine(this), "Failed to setup solver '" + solver + "'.", null, solver);
        }
        String gameDir = "results/" + gameDesc.getConfigString();
        String solverDir =  gameDir + "/" + usedSolverFactory.getConfigString();
        new File(solverDir).mkdirs();
        System.out.println(String.format("Solving %s with %s logged to %s.", gameDesc.getConfigString(), usedSolverFactory.getConfigString(), solverDir));
        Strategy bestStrategy = null;
        double bestStrategyExp = Double.MAX_VALUE;
        final int evalEntriesCount = (int)(timeLimit*1000/evalFreq);
        for (int i = 0; i < count; ++i) {
            String csvFileName = solverDir + "/" + getDateKey() + ".csv";
            CSVPrinter csvOut;
            try {
                csvOut = new CSVPrinter(new FileWriter(csvFileName),
                        CSVFormat.EXCEL.withHeader("intended_time", "time", "iterations", "states", "exp", "avg_regret"));
                BaseCFRSolver cfrSolver = usedSolverFactory.create(null);
                IGameTraversalTracker tracker = SimpleTracker.createRoot(gameDesc.getInitialState());
                int entryIdx = 0;
                final long evaluateAfterMs = evalFreq;
                StopWatch timer = new StopWatch(), evaluationTimer = new StopWatch();
                timer.start();
                evaluationTimer.start();
                long iter = 0;
                double strategyExp = 0;
                while (entryIdx < evalEntriesCount) {
                    while (evaluationTimer.getLiveDurationMs() < evaluateAfterMs) {
                        iter++;
                        cfrSolver.runIteration(tracker);
                    }

                    timer.stop();
                    long visitedStates = cfrSolver.getVisitedStates();
                    double exp = ImperfectRecallExploitability.computeExploitability(new NormalizingStrategyWrapper(cfrSolver.getCumulativeStrat()), gameDesc);
                    strategyExp = exp;
                    double avgRegret = cfrSolver.getTotalRegret() / iter;
                    csvOut.printRecord((entryIdx+1) * evaluateAfterMs ,timer.getDurationMs(), iter, visitedStates, exp, avgRegret);
                    csvOut.flush();
                    if (count == 1) {
                        System.out.println(String.format("(%8d ms, %10d iterations, %12d states) -> (%.4f exp, %.4f avg. regret)",
                                timer.getDurationMs(), iter, visitedStates, exp, avgRegret));
                    }
                    entryIdx++;
                    evaluationTimer.reset();
                    timer.start();
                }
                if (saveStrategy && strategyExp < bestStrategyExp) {
                    bestStrategy = cfrSolver.getFinalCumulativeStrat();
                    bestStrategyExp = strategyExp;
                }
                csvOut.close();
            } catch (IOException e) {
                continue;
            }
        }


        if (saveStrategy && bestStrategy != null) {
            System.out.println(String.format("Best exploitability estimate: %f", bestStrategyExp));
            bestStrategy.normalize();
            try {
                String resFileName = String.format("%1$s/%2$s-solution-%3$.4f.strat", gameDir, getDateKey(),  bestStrategyExp);
                System.out.println("Saving to: " + resFileName);
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

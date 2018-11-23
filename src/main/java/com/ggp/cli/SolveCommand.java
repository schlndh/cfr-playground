package com.ggp.cli;

import com.ggp.*;
import com.ggp.parsers.ParseUtils;
import com.ggp.parsers.exceptions.ConfigAssemblyException;
import com.ggp.players.deepstack.trackers.IGameTraversalTracker;
import com.ggp.players.deepstack.trackers.SimpleTracker;
import com.ggp.players.deepstack.utils.Strategy;
import com.ggp.solvers.cfr.BaseCFRSolver;
import com.ggp.utils.NormalizingStrategyWrapper;
import com.ggp.utils.time.StopWatch;
import com.ggp.utils.recall.ImperfectRecallExploitability;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import picocli.CommandLine;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.ObjectOutputStream;
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

    @CommandLine.Option(names={"--save-strategy"}, description="Save computed strategy", defaultValue = "false")
    private boolean saveStrategy;

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
        BaseCFRSolver cfrSolver = usedSolverFactory.create(null);
        IGameTraversalTracker tracker = SimpleTracker.createRoot(gameDesc.getInitialState());
        String resDir = "results/" + gameDesc.getConfigString();
        new File(resDir).mkdirs();
        String dateKey = String.format("%1$tY%1$tm%1$td-%1$tH%1$tM%1$tS", new Date());
        String csvFileName = resDir + "/" + String.format("%s-%s.csv", usedSolverFactory.getConfigString(), dateKey);
        System.out.println(String.format("Solving %s with %s logged to %s.", gameDesc.getConfigString(), usedSolverFactory.getConfigString(), csvFileName));
        try {
            CSVPrinter csvOut = new CSVPrinter(new FileWriter(csvFileName), CSVFormat.EXCEL.withHeader("time", "iterations", "states", "exp", "avg_regret"));
            final long evaluateAfterMs = evalFreq;
            StopWatch timer = new StopWatch(), evaluationTimer = new StopWatch();
            timer.start();
            evaluationTimer.start();
            long i = 0;
            while (timer.getLiveDurationMs()/1000 < timeLimit) {
                i++;
                cfrSolver.runIteration(tracker);
                if (evaluationTimer.getLiveDurationMs() > evaluateAfterMs) {
                    timer.stop();
                    long visitedStates = cfrSolver.getVisitedStates();
                    double exp = ImperfectRecallExploitability.computeExploitability(new NormalizingStrategyWrapper(cfrSolver.getCumulativeStrat()), gameDesc);
                    double avgRegret = cfrSolver.getTotalRegret() / i;
                    csvOut.printRecord(timer.getDurationMs(), i, visitedStates, exp, avgRegret);
                    System.out.println(String.format("(%8d ms, %10d iterations, %12d states) -> (%.4f exp, %.4f avg. regret)", timer.getDurationMs(), i, visitedStates, exp, avgRegret));
                    evaluationTimer.reset();
                    timer.start();
                }
            }
            csvOut.close();
        } catch (Exception e) {
            System.out.print(e.getMessage());
            return;
        }


        if (saveStrategy) {
            double exp = ImperfectRecallExploitability.computeExploitability(new NormalizingStrategyWrapper(cfrSolver.getCumulativeStrat()), gameDesc);
            System.out.println(String.format("Final exploitability estimate: %f", exp));
            (cfrSolver.getCumulativeStrat()).normalize();
            try {
                String resFileName = String.format("%1$s/%2$s-solution-%3$.4f.strat", resDir, dateKey,  exp);
                System.out.println("Saving to: " + resFileName);
                FileOutputStream fileOutputStream = new FileOutputStream(resFileName);
                ObjectOutputStream objectOutputStream
                        = new ObjectOutputStream(fileOutputStream);
                objectOutputStream.writeObject(cfrSolver.getCumulativeStrat());
                objectOutputStream.flush();
                objectOutputStream.close();
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }

    }
}

package com.ggp.cli;

import com.ggp.GameManager;
import com.ggp.IGameDescription;
import com.ggp.IPlayerFactory;
import com.ggp.parsers.ParseUtils;
import com.ggp.parsers.exceptions.ConfigAssemblyException;
import com.ggp.players.deepstack.DeepstackPlayer;
import com.ggp.players.deepstack.ISubgameResolver;
import com.ggp.players.deepstack.evaluators.EvaluatorEntry;
import com.ggp.players.deepstack.evaluators.IDeepstackEvaluator;
import com.ggp.players.deepstack.resolvers.ExternalCFRResolver;
import com.ggp.solvers.cfr.BaseCFRSolver;
import com.ggp.utils.exploitability.ExploitabilityUtils;
import com.ggp.utils.strategy.Strategy;
import picocli.CommandLine;

import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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

    @CommandLine.Option(names={"-s", "--solver"}, description="CFR solver")
    private String solver;

    @CommandLine.Option(names={"-e", "--evaluator"}, description="Evaluator")
    private String evaluator;

    @CommandLine.Option(names={"-i", "--init"}, description="Init time (ms)", required=true)
    private int init;

    @CommandLine.Option(names={"-t", "--time-limit"}, description="Time per move (ms)", required=true)
    private int timeLimit;

    @CommandLine.Option(names={"-f", "--eval-freq"}, description="Evaluation frequency (ms)", required=true)
    private int evalFreq;

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

        IDeepstackEvaluator.IFactory usedEvaluatorFactory = null;
        try {
            usedEvaluatorFactory = mainCommand.getConfigurableFactory().create(IDeepstackEvaluator.IFactory.class, ParseUtils.parseConfigExpression(evaluator));
        } catch (ConfigAssemblyException e) {}

        if (usedEvaluatorFactory == null) {
            throw new CommandLine.ParameterException(new CommandLine(this), "Failed to setup evalautor '" + evaluator + "'.", null, evaluator);
        }

        ISubgameResolver.Factory usedResolver = new ExternalCFRResolver.Factory(usedSolverFactory, useISTargeting);
        if (!quiet) System.out.println("Exploitability estimate for uniform strategy: " + ExploitabilityUtils.computeExploitability(new Strategy(), gameDesc));

        ArrayList<Integer> logPointsMs = new ArrayList<>();
        int logTimeMs = evalFreq;
        while (logTimeMs < timeLimit) {
            logPointsMs.add(logTimeMs);
            logTimeMs += evalFreq;
        }
        logPointsMs.add(timeLimit);
        IDeepstackEvaluator evaluator = usedEvaluatorFactory.create(init, logPointsMs);

        String gameDir = resultsDirectory + "/" + gameDesc.getConfigString();
        String solverDir =  gameDir + "/" + usedSolverFactory.getConfigString();
        if (!dryRun)
            new File(solverDir).mkdirs();

        if (!quiet) System.out.println("Evaluating " + usedResolver.getConfigString() + " using " + evaluator.getConfigString());
        warmup(gameDesc, new DeepstackPlayer.Factory(usedResolver));
        List<EvaluatorEntry> entries = evaluator.evaluate(gameDesc, usedResolver, quiet);

        long lastEntryStates = 0;
        double lastTime = 0;
        Strategy bestStrategy = null;
        double bestStrategyExp = Double.MAX_VALUE;
        for (EvaluatorEntry entry : entries) {
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

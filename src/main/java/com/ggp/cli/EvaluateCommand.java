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
import com.ggp.utils.exploitability.ImperfectRecallExploitability;
import com.ggp.utils.strategy.Strategy;
import picocli.CommandLine;

import java.util.ArrayList;
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

    private void warmup(IGameDescription gameDesc, IPlayerFactory plFactory) {
        if (!quiet) System.out.println("Warming up...");
        GameManager manager = new GameManager(plFactory, plFactory, gameDesc);
        manager.run(300, 300);
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

        ISubgameResolver.Factory usedResolver = new ExternalCFRResolver.Factory(usedSolverFactory);
        if (!quiet) System.out.println("Exploitability estimate for uniform strategy: " + ImperfectRecallExploitability.computeExploitability(new Strategy(), gameDesc));

        ArrayList<Integer> logPointsMs = new ArrayList<>();
        int logTimeMs = evalFreq;
        while (logTimeMs < timeLimit) {
            logPointsMs.add(logTimeMs);
            logTimeMs += evalFreq;
        }
        logPointsMs.add(timeLimit);
        IDeepstackEvaluator evaluator = usedEvaluatorFactory.create(init, logPointsMs);

        if (!quiet) System.out.println("Evaluating " + usedResolver.getConfigString() + " using " + evaluator.getConfigString());
        warmup(gameDesc, new DeepstackPlayer.Factory(usedResolver));
        List<EvaluatorEntry> entries = evaluator.evaluate(gameDesc, usedResolver, quiet);

        long lastEntryStates = 0;
        for (EvaluatorEntry entry : entries) {
            double exp = ImperfectRecallExploitability.computeExploitability(entry.getAggregatedStrat(), gameDesc, null);
            if (!quiet) {
                System.out.println(String.format("(%5d ms, %12d states) -> %.4f exp | %.4g states/s",
                        (int) entry.getEntryTimeMs(), entry.getAvgVisitedStates(), exp, 1000*(entry.getAvgVisitedStates() - lastEntryStates)/(double)(evalFreq)));
            }
            lastEntryStates = entry.getAvgVisitedStates();
        }
    }
}

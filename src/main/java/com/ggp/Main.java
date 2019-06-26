package com.ggp;

import com.ggp.cli.MainCommand;
import com.ggp.parsers.*;
import com.ggp.players.PerfectRecallPlayerFactory;
import com.ggp.players.StrategyBasedPlayer;
import com.ggp.players.continual_resolving.ContinualResolvingPlayer;
import com.ggp.players.continual_resolving.utils.ContinualResolvingkUtilityEstimatorWrapper;
import com.ggp.players.solving.SolvingPlayer;
import com.ggp.player_evaluators.GamePlayingEvaluator;
import com.ggp.player_evaluators.IPlayerEvaluator;
import com.ggp.player_evaluators.TraversingEvaluator;
import com.ggp.solvers.cfr.IRegretMatching;
import com.ggp.players.continual_resolving.ISubgameResolver;
import com.ggp.solvers.cfr.regret_matching.DiscountedRegretMatching;
import com.ggp.solvers.cfr.regret_matching.ExplorativeRegretMatching;
import com.ggp.solvers.cfr.regret_matching.RegretMatching;
import com.ggp.solvers.cfr.regret_matching.RegretMatchingPlus;
import com.ggp.players.random.RandomPlayer;
import com.ggp.solvers.cfr.*;
import com.ggp.solvers.cfr.baselines.ExponentiallyDecayingAverageBaseline;
import com.ggp.solvers.cfr.baselines.NoBaseline;
import com.ggp.utils.GameRepository;
import com.ggp.utils.IUtilityEstimator;
import com.ggp.utils.estimators.RandomPlayoutUtilityEstimator;
import com.ggp.utils.game.CheckedTraversal.CheckedTraversalGameDescription;
import com.ggp.utils.game.PlayerSwap.PlayerSwapGameDescription;
import com.ggp.utils.recall.PerfectRecallGameDescriptionWrapper;
import picocli.CommandLine;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class Main {

    public static void main(String[] args) throws NoSuchMethodException {
        MainCommand main = new MainCommand();
        ConfigurableFactory factory = main.getConfigurableFactory();
        registerClassesToFactory(factory);

        CommandLine cli = new CommandLine(main);
        for (Class<?> c: factory.getRegisteredTypes()) {
            Class<Object> cls = (Class<Object>) c;
            cli.registerConverter(cls, configKey -> {
                ConfigExpression expr = ParseUtils.parseConfigExpression(configKey);
                if (expr == null) throw new CommandLine.ParameterException(cli, "Invalid config key!");
                return factory.create(cls, expr);
            });
        }

        // can't use cli.run(main, args) because it re-creates the commands without the registered type converters
        CommandLine.Help.Ansi ansi = CommandLine.Help.Ansi.AUTO;
        cli.parseWithHandlers(new CommandLine.RunLast().useOut(System.out).useAnsi(ansi),
                new CommandLine.DefaultExceptionHandler<List<Object>>().useErr(System.err).useAnsi(ansi), args);
    }

    public static void registerClassesToFactory(ConfigurableFactory factory) throws NoSuchMethodException {
        registerGames(factory);
        registerRegretMatchings(factory);
        registerUtilityEstimators(factory);
        registerCFRSolvers(factory);
        registerPlayers(factory);
        registerBaselines(factory);
        registerPlayerEvaluators(factory);
    }

    private static void registerGames(ConfigurableFactory factory) throws NoSuchMethodException {
        factory.setTypeDescription(IGameDescription.class,  "Game description");

        factory.register(IGameDescription.class, "LeducPoker",
                ConfigurableFactory.createPositionalParameterList(
                        GameRepository.leducPoker(7).getClass().getConstructor(int.class),
                        "Starting money (M)",
                        "Shortcut for LeducPoker{M,M,1,3}"
                )
        );
        factory.register(IGameDescription.class, "LeducPoker",
                ConfigurableFactory.createPositionalParameterList(
                        GameRepository.leducPoker(7).getClass().getConstructor(int.class, int.class, int.class, int.class),
                        "Starting money for player 1", "Starting money for player 2",
                        "Number of allowed raises per betting round", "Number of different cards in each of the two suites",
                        "Full Leduc poker constructor"
                )
        );
        factory.setImplementationDescription(IGameDescription.class, "LeducPoker","Imperfect recall Leduc poker");

        factory.register(IGameDescription.class, "IIGoofspiel",
                ConfigurableFactory.createPositionalParameterList(
                        GameRepository.iiGoofspiel(5).getClass().getConstructor(int.class),
                        "Number of cards in each deck (N)"
                ),
                "Perfect recall implementation of II-Goofspiel"
        );

        factory.register(IGameDescription.class, "RockPaperScissors",
                ConfigurableFactory.createPositionalParameterList(
                        GameRepository.rps(5).getClass().getConstructor(int.class),
                        "Number of possible moves (must be odd)"
                ),
                "Perfect recall implementation of Rock-Paper-Scissors with variable size."
        );

        factory.register(IGameDescription.class, "PerfectRecall",
                ConfigurableFactory.createPositionalParameterList(
                        PerfectRecallGameDescriptionWrapper.class.getConstructor(IGameDescription.class),
                        "Game to wrap"
                ),
                "Wrapper that adds perfect recall to a game"
        );

        factory.register(IGameDescription.class, "PrincessAndMonster",
                ConfigurableFactory.createPositionalParameterList(
                        GameRepository.princessAndMonster(5).getClass().getConstructor(int.class),
                        "Number of max. turns per player"
                ),
                "Imperfect recall implementation of princess and monster on a 3x3 grid without diagonal connections"
        );
        factory.register(IGameDescription.class, "LatentTicTacToe",
                ConfigurableFactory.createPositionalParameterList(
                        GameRepository.latentTTT().getClass().getConstructor()
                ),
                "Imperfect recall implementation of latent tic-tac-toe on a 3x3 grid"
        );
        factory.register(IGameDescription.class, "PlayerSwap",
                ConfigurableFactory.createPositionalParameterList(
                        PlayerSwapGameDescription.class.getConstructor(IGameDescription.class),
                        "Game to wrap"
                ),
                "Wrapper that adds an initial chance node which swaps player roles with 50% probability"
        );
        factory.register(IGameDescription.class, "CheckedTraversal",
                ConfigurableFactory.createPositionalParameterList(
                        CheckedTraversalGameDescription.class.getConstructor(IGameDescription.class),
                        "Game to wrap"
                ),
                "Wrapper that checks the validity of used actions and percepts. Useful for debugging."
        );
    }

    private static void registerCFRSolvers(ConfigurableFactory factory) {
        factory.setTypeDescription(BaseCFRSolver.Factory.class, "CFR solver factory");
        Parameter rm = new Parameter(IRegretMatching.IFactory.class, null, true, "Regret matching");
        Parameter cse = new Parameter(double.class, 0d, false, "Cumulative strategy discounting exponent");
        {
            HashMap<String, Parameter> params = new HashMap<>();
            params.put("rm", rm);
            params.put("ue", new Parameter(IUtilityEstimator.IFactory.class, null, false, "Utility estimator"));
            params.put("dl", new Parameter(int.class, 0, false, "Depth limit"));
            params.put("au", new Parameter(boolean.class, true, false, "Alternating updates"));
            params.put("cse", cse);
            factory.register(BaseCFRSolver.Factory.class, "CFR", new ParameterList(null, params,
                    (posParams, kvParams) -> new DepthLimitedCFRSolver.Factory(
                            (IRegretMatching.IFactory) kvParams.get("rm"),
                            (int) kvParams.get("dl"),
                            (IUtilityEstimator.IFactory) kvParams.get("ue"),
                            (boolean) kvParams.get("au"),
                            (double) kvParams.get("cse")
                    )
            ), "Depth-limited CFR");
        }
        Parameter e = new Parameter(double.class, 0.2d, false, "Exploration probability");
        Parameter t = new Parameter(double.class, 0d, false, "Targeting probability");
        {
            HashMap<String, Parameter> params = new HashMap<>();
            params.put("rm", rm);
            params.put("e", e);
            params.put("t", t);
            params.put("cse", cse);
            factory.register(BaseCFRSolver.Factory.class, "MC-CFR", new ParameterList(null, params,
                    (posParams, kvParams) -> new MCCFRSolver.Factory(
                            (IRegretMatching.IFactory) kvParams.get("rm"),
                            (double) kvParams.get("e"),
                            (double) kvParams.get("t"),
                            (double) kvParams.get("cse")
                    )
            ), "Monte-Carlo CFR");
        }
        {
            HashMap<String, Parameter> params = new HashMap<>();
            params.put("rm", rm);
            params.put("bl", new Parameter(IBaseline.IFactory.class, null, true, "Baseline"));
            params.put("e", e);
            params.put("t", t);
            params.put("cse", cse);
            factory.register(BaseCFRSolver.Factory.class, "VR-MCCFR", new ParameterList(null, params,
                    (posParams, kvParams) -> new VRMCCFRSolverFactory(
                            (IRegretMatching.IFactory) kvParams.get("rm"),
                            (double) kvParams.get("e"),
                            (double) kvParams.get("t"),
                            (double) kvParams.get("cse"),
                            (IBaseline.IFactory) kvParams.get("bl")
                    )
            ), "Variance-Reduction Monte-Carlo CFR");
        }
    }

    private static void registerPlayers(ConfigurableFactory factory) throws NoSuchMethodException {
        factory.setTypeDescription(IPlayerFactory.class, "Player factory");

        factory.register(IPlayerFactory.class, "RandomPlayer",
                new ParameterList(null, null, (a, b) -> new RandomPlayer.Factory()),
                "Uniform random player"
        );
        factory.register(IPlayerFactory.class, "ContinualResolving", ConfigurableFactory.createPositionalParameterList(
                ContinualResolvingPlayer.Factory.class.getConstructor(BaseCFRSolver.Factory.class),
                "CFR solver used for re-solving",
                "Continual resolving player with default resolver"
        ), "Continual resolving player");
        factory.register(IPlayerFactory.class, "ContinualResolving", ConfigurableFactory.createPositionalParameterList(
                ContinualResolvingPlayer.Factory.class.getConstructor(ISubgameResolver.IFactory.class),
                "Subgame resolver",
                "Continual resolving player with custom resolver"
        ));
        factory.register(IPlayerFactory.class, "PerfectRecall", ConfigurableFactory.createPositionalParameterList(
                PerfectRecallPlayerFactory.class.getConstructor(IPlayerFactory.class),
                "Player to wrap"
        ), "Wrapper that passes a perfect recall version of the game to the wrapped player");
        factory.register(IPlayerFactory.class, "SolvingPlayer", ConfigurableFactory.createPositionalParameterList(
                SolvingPlayer.Factory.class.getConstructor(BaseCFRSolver.Factory.class),
                "Used CFR solver"
        ), "Player that solves the game while playing");
        factory.register(IPlayerFactory.class, "StrategyBasedPlayer", ConfigurableFactory.createPositionalParameterList(
                StrategyBasedPlayer.DynamiclyLoadedStrategyFactory.class.getConstructor(String.class),
                "Directory with serialized strategies (filenames in format of GAME.strat)"
        ), "Player that uses a precomputed strategy to play");
    }

    private static void registerRegretMatchings(ConfigurableFactory factory) throws NoSuchMethodException {
        factory.setTypeDescription(IRegretMatching.IFactory.class, "Regret matching factory");

        factory.register(IRegretMatching.IFactory.class, "RM",
                new ParameterList(null, null, (a, b) -> new RegretMatching.Factory()),
                "Classical regret matching");
        factory.register(IRegretMatching.IFactory.class, "RM+",
                new ParameterList(null, null, (a, b) -> new RegretMatchingPlus.Factory()),
                "Regret matching+");
        factory.register(IRegretMatching.IFactory.class, "DRM", ConfigurableFactory.createPositionalParameterList(
                DiscountedRegretMatching.Factory.class.getConstructor(double.class, double.class),
                "Discounting exponent for positive regret", "Discounting exponent for negative regret"
        ), "Discounted regret matching");
        factory.register(IRegretMatching.IFactory.class, "ERM", ConfigurableFactory.createPositionalParameterList(
                ExplorativeRegretMatching.Factory.class.getConstructor(IRegretMatching.IFactory.class, double.class),
                "Underlying regret matching", "Uniform strategy weight"
        ), "Wrapper that adds a uniform strategy to the one produced by the underlying regret matching with the given weight");
    }

    private static void registerUtilityEstimators(ConfigurableFactory factory) throws NoSuchMethodException {
        factory.setTypeDescription(IUtilityEstimator.IFactory.class, "Game-state utility estimator factory");

        factory.register(IUtilityEstimator.IFactory.class, "RandomPlayout",
                new ParameterList(Arrays.asList(new Parameter(int.class, null, true, "Number of random playouts used to get an estimate")), null,
                        (posParams, kvParams) -> new RandomPlayoutUtilityEstimator.Factory((int)posParams.get(0))),
                "Uniform random playout estimator");
        factory.register(IUtilityEstimator.IFactory.class, "CRUEW", ConfigurableFactory.createPositionalParameterList(
                ContinualResolvingkUtilityEstimatorWrapper.Factory.class.getConstructor(IUtilityEstimator.IFactory.class),
                "Underlying utility estimator"
        ), "Wrapper that makes sure that depth-limited CFR visits the states required by continual resolving (player's next turn in a different subgame).");
    }

    private static void registerBaselines(ConfigurableFactory factory) throws NoSuchMethodException {
        factory.setTypeDescription(IBaseline.IFactory.class, "VR-MCCFR baseline factory");

        factory.register(IBaseline.IFactory.class, "ExpAvg",
                ConfigurableFactory.createPositionalParameterList(
                        ExponentiallyDecayingAverageBaseline.Factory.class.getConstructor(double.class),
                        "Exponential decay factor"
                ),
                "Exponentially decaying average baseline"
        );
        factory.register(IBaseline.IFactory.class, "None",
                new ParameterList(null, null, (a, b) -> new NoBaseline.Factory()),
                "Empty baseline");
    }

    private static void registerPlayerEvaluators(ConfigurableFactory factory) throws NoSuchMethodException {
        factory.setTypeDescription(IPlayerEvaluator.IFactory.class,"Player evaluator factory");

        factory.register(IPlayerEvaluator.IFactory.class, "GamePlayingEvaluator",
                ConfigurableFactory.createPositionalParameterList(
                        GamePlayingEvaluator.Factory.class.getConstructor(int.class),
                        "Number of games to play"
                ),
                "Game-playing evaluator"
        );
        factory.register(IPlayerEvaluator.IFactory.class, "GamePlayingEvaluator",
                ConfigurableFactory.createPositionalParameterList(
                        GamePlayingEvaluator.Factory.class.getConstructor(int.class, int.class),
                        "Number of games to play", "Player's role in the first game (1/2)"
                ),
                "Game-playing evaluator"
        );
        factory.register(IPlayerEvaluator.IFactory.class, "TraversingEvaluator",
                ConfigurableFactory.createPositionalParameterList(
                        TraversingEvaluator.Factory.class.getConstructor()
                ),
                "Traversing evaluator"
        );
    }
}

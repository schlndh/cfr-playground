package com.ggp;

import com.ggp.cli.MainCommand;
import com.ggp.parsers.ConfigurableFactory;
import com.ggp.parsers.Parameter;
import com.ggp.parsers.ParameterList;
import com.ggp.players.PerfectRecallPlayerFactory;
import com.ggp.players.deepstack.DeepstackPlayer;
import com.ggp.players.deepstack.DeepstackPlayerCommand;
import com.ggp.players.deepstack.IRegretMatching;
import com.ggp.players.deepstack.ISubgameResolver;
import com.ggp.players.deepstack.regret_matching.RegretMatching;
import com.ggp.players.deepstack.regret_matching.RegretMatchingPlus;
import com.ggp.players.random.RandomPlayer;
import com.ggp.players.random.RandomPlayerCommand;
import com.ggp.solvers.cfr.*;
import com.ggp.solvers.cfr.baselines.ExponentiallyDecayingAverageBaseline;
import com.ggp.solvers.cfr.baselines.NoBaseline;
import com.ggp.utils.GameRepository;
import com.ggp.utils.IUtilityEstimator;
import com.ggp.utils.estimators.RandomPlayoutUtilityEstimator;
import com.ggp.utils.recall.PerfectRecallGameDescriptionWrapper;
import picocli.CommandLine;

import java.util.Arrays;
import java.util.HashMap;
import java.util.function.Consumer;

public class Main {

    public static void main(String[] args) throws NoSuchMethodException {
        MainCommand main = new MainCommand();
        ConfigurableFactory factory = main.getConfigurableFactory();
        registerGames(factory);
        registerRegretMatchings(factory);
        registerUtilityEstimators(factory);
        registerCFRSolvers(factory);
        registerPlayers(factory);
        registerBaselines(factory);

        CommandLine cli = new CommandLine(main);
        cli.run(main, args);

    }

    private static void registerGames(ConfigurableFactory factory) throws NoSuchMethodException {
        factory.register(IGameDescription.class, "LeducPoker",
                ConfigurableFactory.createPositionalParameterList(
                        GameRepository.leducPoker(7,7).getClass().getConstructor(int.class, int.class)
                )
        );
        factory.register(IGameDescription.class, "IIGoofspiel",
                ConfigurableFactory.createPositionalParameterList(
                        GameRepository.iiGoofspiel(5).getClass().getConstructor(int.class)
                )
        );
        factory.register(IGameDescription.class, "RockPaperScissors",
                ConfigurableFactory.createPositionalParameterList(
                        GameRepository.rps(5).getClass().getConstructor(int.class)
                )
        );
        factory.register(IGameDescription.class, "KriegTicTacToe",
                ConfigurableFactory.createPositionalParameterList(
                        GameRepository.kriegTTT().getClass().getConstructor()
                )
        );
        factory.register(IGameDescription.class, "PerfectRecall",
                ConfigurableFactory.createPositionalParameterList(
                        PerfectRecallGameDescriptionWrapper.class.getConstructor(IGameDescription.class)
                )
        );
    }

    private static void registerCFRSolvers(ConfigurableFactory factory) {
        {
            HashMap<String, Parameter> params = new HashMap<>();
            params.put("rm", new Parameter(IRegretMatching.Factory.class, null, true));
            params.put("ue", new Parameter(IUtilityEstimator.IFactory.class, null, false));
            params.put("dl", new Parameter(int.class, 0, false));
            factory.register(BaseCFRSolver.Factory.class, "CFR", new ParameterList(null, params,
                    (posParams, kvParams) -> new DepthLimitedCFRSolver.Factory(
                            (IRegretMatching.Factory) kvParams.get("rm"),
                            (int) kvParams.get("dl"),
                            (IUtilityEstimator.IFactory) kvParams.get("ue")
                    )
            ));
        }
        {
            HashMap<String, Parameter> params = new HashMap<>();
            params.put("rm", new Parameter(IRegretMatching.Factory.class, null, true));
            params.put("e", new Parameter(double.class, 0.2d, false));
            params.put("t", new Parameter(double.class, 0d, false));
            factory.register(BaseCFRSolver.Factory.class, "MC-CFR", new ParameterList(null, params,
                    (posParams, kvParams) -> new MCCFRSolver.Factory(
                            (IRegretMatching.Factory) kvParams.get("rm"),
                            (double) kvParams.get("e"),
                            (double) kvParams.get("t")
                    )
            ));
        }
        {
            HashMap<String, Parameter> params = new HashMap<>();
            params.put("rm", new Parameter(IRegretMatching.Factory.class, null, true));
            params.put("bl", new Parameter(IBaseline.IFactory.class, null, true));
            params.put("e", new Parameter(double.class, 0.2d, false));
            params.put("t", new Parameter(double.class, 0d, false));
            factory.register(BaseCFRSolver.Factory.class, "VR-MCCFR", new ParameterList(null, params,
                    (posParams, kvParams) -> new VRMCCFRSolverFactory(
                            (IRegretMatching.Factory) kvParams.get("rm"),
                            (double) kvParams.get("e"),
                            (double) kvParams.get("t"),
                            (IBaseline.IFactory) kvParams.get("bl")
                    )
            ));
        }
    }

    private static void registerPlayers(ConfigurableFactory factory) throws NoSuchMethodException {
        factory.register(IPlayerFactory.class, "RandomPlayer",
                new ParameterList(null, null, (a, b) -> new RandomPlayer.Factory())
        );
        factory.register(IPlayerFactory.class, "Deepstack", ConfigurableFactory.createPositionalParameterList(
                DeepstackPlayer.Factory.class.getConstructor(BaseCFRSolver.Factory.class)
        ));
        factory.register(IPlayerFactory.class, "Deepstack", ConfigurableFactory.createPositionalParameterList(
                DeepstackPlayer.Factory.class.getConstructor(ISubgameResolver.Factory.class)
        ));
        factory.register(IPlayerFactory.class, "PerfectRecall", ConfigurableFactory.createPositionalParameterList(
                PerfectRecallPlayerFactory.class.getConstructor(IPlayerFactory.class)
        ));
    }

    private static void registerRegretMatchings(ConfigurableFactory factory) {
        factory.register(IRegretMatching.Factory.class, "RM",
                new ParameterList(null, null, (a, b) -> new RegretMatching.Factory()));
        factory.register(IRegretMatching.Factory.class, "RM+",
                new ParameterList(null, null, (a, b) -> new RegretMatchingPlus.Factory()));
    }

    private static void registerUtilityEstimators(ConfigurableFactory factory) {
        factory.register(IUtilityEstimator.IFactory.class, "RandomPlayout",
                new ParameterList(Arrays.asList(new Parameter(int.class, null, true)), null,
                        (posParams, kvParams) -> new RandomPlayoutUtilityEstimator.Factory((int)posParams.get(0))));
    }

    private static void registerBaselines(ConfigurableFactory factory) throws NoSuchMethodException {
        factory.register(IBaseline.IFactory.class, "ExpAvg",
                ConfigurableFactory.createPositionalParameterList(
                        ExponentiallyDecayingAverageBaseline.Factory.class.getConstructor(double.class)
                )
        );
        factory.register(IBaseline.IFactory.class, "None",
                new ParameterList(null, null, (a, b) -> new NoBaseline.Factory()));
    }
}

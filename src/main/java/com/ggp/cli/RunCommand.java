package com.ggp.cli;

import com.ggp.*;
import com.ggp.parsers.ParseUtils;
import com.ggp.parsers.exceptions.WrongConfigKeyException;
import com.ggp.utils.DefaultStateVisualizer;
import picocli.CommandLine;

@CommandLine.Command(name = "run",
        mixinStandardHelpOptions = true,
        description = "Runs given game with given players",
        optionListHeading = "%nOptions:%n",
        sortOptions = false
)
public class RunCommand implements Runnable {
    @CommandLine.ParentCommand
    private MainCommand mainCommand;

    @CommandLine.Option(names={"-g", "--game"}, description="game to be played", required=true)
    private String game;

    @CommandLine.Option(names={"--player1"}, description="player 1", required=true)
    private String player1;

    @CommandLine.Option(names={"--player2"}, description="player 2", required=true)
    private String player2;

    private IPlayerFactory getPlayerFactory(String player) {
        IPlayerFactory pl = null;
        try {
            pl = (IPlayerFactory) mainCommand.getConfigurableFactory().create(IPlayerFactory.class, ParseUtils.parseConfigKey(player));
        } catch (WrongConfigKeyException e) { }

        if (pl == null) {
            throw new CommandLine.ParameterException(new CommandLine(this), "Failed to setup player '" + player + "'.", null, player);
        }
        return pl;
    }

    @Override
    public void run() {
        IGameDescription gameDesc = null;
        try {
            gameDesc = (IGameDescription) mainCommand.getConfigurableFactory().create(IGameDescription.class, ParseUtils.parseConfigKey(game));
        } catch (WrongConfigKeyException e) { }

        if (gameDesc == null) {
            throw new CommandLine.ParameterException(new CommandLine(this), "Failed to setup game '" + game + "'.", null, game);
        }
        IStateVisualizer visualizer = new DefaultStateVisualizer();

        IPlayerFactory pl1 = getPlayerFactory(player1);
        IPlayerFactory pl2 = getPlayerFactory(player2);
        System.out.println(String.format("%s: %s vs %s", gameDesc.getConfigString(), pl1.getConfigString(), pl2.getConfigString()));

        GameManager manager = new GameManager(pl1, pl2, gameDesc);
        if (visualizer != null) {
            manager.registerGameListener(new GamePlayVisualizer(visualizer));
        }

        manager.run(1000, 1000);
        System.out.println("Result 1:" + manager.getPayoff(1) + ", 2:" + manager.getPayoff(2));

    }
}

package com.ggp.cli;

import com.ggp.*;
import com.ggp.utils.DefaultStateVisualizer;
import picocli.CommandLine;

@CommandLine.Command(name = "run",
        mixinStandardHelpOptions = true,
        description = "Runs given game with given players",
        optionListHeading = "%nOptions:%n",
        sortOptions = false
)
class RunCommand implements Runnable {
    @CommandLine.ParentCommand
    private MainCommand mainCommand;

    @CommandLine.Option(names={"-g", "--game"}, description="game to be played (IGameDescription)", required=true)
    private IGameDescription game;

    @CommandLine.Option(names={"--player1"}, description="player 1 (IPlayerFactory)", required=true)
    private IPlayerFactory player1;

    @CommandLine.Option(names={"--player2"}, description="player 2 (IPlayerFactory)", required=true)
    private IPlayerFactory player2;

    @Override
    public void run() {
        if (game == null) {
            System.out.println("Game can't be null!");
            return;
        }
        if (player1 == null || player2 == null) {
            System.out.println("Players can't be null!");
            return;
        }
        IStateVisualizer visualizer = new DefaultStateVisualizer();
        System.out.println(String.format("%s: %s vs %s", game.getConfigString(), player1.getConfigString(), player2.getConfigString()));

        GameManager manager = new GameManager(player1, player2, game);
        if (visualizer != null) {
            manager.registerGameListener(new GamePlayVisualizer(visualizer));
        }

        manager.run(1000, 1000);
        System.out.println("Result 1:" + manager.getPayoff(1) + ", 2:" + manager.getPayoff(2));

    }
}

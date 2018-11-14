package com.ggp.games.IIGoofspiel;

import com.ggp.IGameDescription;
import com.ggp.IStateVisualizer;
import com.ggp.cli.IGameCommand;
import com.ggp.utils.DefaultStateVisualizer;
import picocli.CommandLine;

@CommandLine.Command(
        name = "iigoofspiel",
        description = "II Goofspiel"
)
public class IIGoofspielCommand implements IGameCommand {
    @CommandLine.Parameters(index = "0", paramLabel = "SIZE", description = "Number of cards")
    private int gameSize;

    @Override
    public IGameDescription getGameDescription() {
        return new GameDescription(gameSize);
    }

    @Override
    public IStateVisualizer getStateVisualizer() {
        return new DefaultStateVisualizer();
    }
}

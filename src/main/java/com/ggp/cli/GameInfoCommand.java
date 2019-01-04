package com.ggp.cli;

import com.ggp.*;
import com.ggp.parsers.ParseUtils;
import com.ggp.parsers.exceptions.ConfigAssemblyException;
import com.ggp.utils.DefaultStateVisualizer;
import picocli.CommandLine;

import java.util.HashSet;

@CommandLine.Command(name = "game-info",
        mixinStandardHelpOptions = true,
        description = "Show game info",
        optionListHeading = "%nOptions:%n",
        sortOptions = false
)
public class GameInfoCommand implements Runnable {
    @CommandLine.ParentCommand
    private MainCommand mainCommand;

    @CommandLine.Parameters(index = "0")
    private String game;

    private long states = 0;
    private HashSet<IInformationSet> infoSets = new HashSet<>();

    public void visit(ICompleteInformationState s) {
        states++;
        if (s.isTerminal()) {
            return;
        }
        if (s.isRandomNode()) {
            for (IRandomNode.IRandomNodeAction rna: s.getRandomNode()) {
                visit(s.next(rna.getAction()));
            }
        } else {
            infoSets.add(s.getInfoSetForActingPlayer());
            for (IAction a: s.getLegalActions()) {
                visit(s.next(a));
            }
        }
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
        System.out.println("Game: " + gameDesc.getConfigString());
        ICompleteInformationState s = gameDesc.getInitialState();

        visit(s);
        System.out.println("Game tree size: " + states + ", acting info-sets " + infoSets.size());
    }
}

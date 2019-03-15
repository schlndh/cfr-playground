package com.ggp.cli;

import com.ggp.*;
import com.ggp.parsers.ParseUtils;
import com.ggp.parsers.exceptions.ConfigAssemblyException;
import com.ggp.utils.DefaultStateVisualizer;
import picocli.CommandLine;

import java.util.HashSet;
import java.util.List;

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
    private int maxDepth = 0;
    private double avgDepth = 0;
    private long leaves = 0;
    private long innerStates = 0;
    private double avgBranchingFactor = 0;

    public void visit(ICompleteInformationState s, int depth) {
        states++;
        if (s.isTerminal()) {
            leaves++;
            if (depth > maxDepth) maxDepth = depth;
            avgDepth += (1d/leaves)*(depth - avgDepth);
            return;
        }
        innerStates++;
        List<IAction> legalActions = s.getLegalActions();
        avgBranchingFactor += (1d/innerStates)*(legalActions.size() - avgBranchingFactor);
        if (!s.isRandomNode()) {
            infoSets.add(s.getInfoSetForActingPlayer());
        }
        for (IAction a: legalActions) {
            visit(s.next(a), depth + 1);
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

        visit(s, 0);
        System.out.println(String.format("Game tree size: %d, acting info-sets: %d, max depth: %d, avg. depth: %f, avg. branching factor: %f",
                states, infoSets.size(), maxDepth, avgDepth, avgBranchingFactor));
    }
}

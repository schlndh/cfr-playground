package com.ggp.cli;

import com.ggp.*;
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

    @CommandLine.Parameters(index = "0", description = "game (IGameDescription)")
    private IGameDescription game;

    private long states = 0;
    private HashSet<IInformationSet> infoSets = new HashSet<>();
    private int maxDepth = 0;
    private double avgDepth = 0;
    private long leaves = 0;
    private long innerStates = 0;
    private double avgBranchingFactor = 0;
    private int[] maxActions = new int[] {0, 0, 0};
    private double[] minUtils = new double[] {Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY};
    private double[] maxUtils = new double[] {Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY};

    public void visit(ICompleteInformationState s, int depth) {
        states++;
        if (s.isTerminal()) {
            double[] payoffs = new double[]{0, s.getPayoff(1), s.getPayoff(2)};
            for (int i = 1; i <= 2; ++i) {
                if (payoffs[i] < minUtils[i]) {
                    minUtils[i] = payoffs[i];
                }
                if (payoffs[i] > maxUtils[i]) {
                    maxUtils[i] = payoffs[i];
                }
            }
            leaves++;
            if (depth > maxDepth) maxDepth = depth;
            avgDepth += (1d/leaves)*(depth - avgDepth);
            return;
        }
        innerStates++;
        List<IAction> legalActions = s.getLegalActions();
        avgBranchingFactor += (1d/innerStates)*(legalActions.size() - avgBranchingFactor);
        if (legalActions.size() > maxActions[s.getActingPlayerId()]) {
            maxActions[s.getActingPlayerId()] = legalActions.size();
        }
        if (!s.isRandomNode()) {
            infoSets.add(s.getInfoSetForActingPlayer());
        }
        for (IAction a: legalActions) {
            visit(s.next(a), depth + 1);
        }
}


    @Override
    public void run() {
        if (game == null) {
            System.err.println("Game can't be null!");
            return;
        }
        IGameDescription gameDesc = game;
        System.out.println("Game: " + gameDesc.getConfigString());
        ICompleteInformationState s = gameDesc.getInitialState();

        visit(s, 0);
        double[] utilDelta = new double[]{0, maxUtils[1] - minUtils[1], maxUtils[2] - minUtils[2]};
        System.out.println(String.format("Game tree size: %d, acting info-sets: %d, max depth: %d, avg. depth: %f, " +
                        "avg. branching factor: %f, util delta 1: %f, util delta 2: %f, max actions: %d/%d/%d",
                states, infoSets.size(), maxDepth, avgDepth, avgBranchingFactor, utilDelta[1], utilDelta[2],
                maxActions[0], maxActions[1], maxActions[2]));
    }
}

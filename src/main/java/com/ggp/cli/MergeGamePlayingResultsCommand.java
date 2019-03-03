package com.ggp.cli;

import com.ggp.player_evaluators.GamePlayingEvaluator;
import picocli.CommandLine;

import java.io.File;

@CommandLine.Command(name = "merge-gp-results",
        mixinStandardHelpOptions = true,
        description = "Show game info",
        optionListHeading = "%nOptions:%n",
        sortOptions = false
)
public class MergeGamePlayingResultsCommand implements Runnable {
    @CommandLine.ParentCommand
    private MainCommand mainCommand;

    @CommandLine.Parameters(index = "0")
    private String resultsDir;

    @CommandLine.Option(names={"-d", "--dry-run"})
    private boolean dryRun;

    @Override
    public void run() {
        for (File gameDir: new File(resultsDir).listFiles()) {
            if (!gameDir.isDirectory()) continue;
            System.out.println("Checking " + gameDir.getName());
            for (File solverDir: gameDir.listFiles()) {
                if (!solverDir.isDirectory()) continue;
                System.out.println("\tChecking " + solverDir.getName());
                GamePlayingEvaluator.mergeSavedEntryFiles(solverDir.getAbsolutePath(), dryRun);
            }
        }
    }
}

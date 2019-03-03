package com.ggp.cli;

import com.ggp.IGameDescription;
import com.ggp.parsers.ParseUtils;
import com.ggp.parsers.exceptions.ConfigAssemblyException;
import com.ggp.player_evaluators.EvaluatorEntry;
import com.ggp.player_evaluators.savers.CsvSaver;
import com.ggp.player_evaluators.savers.GamePlayingSaver;
import com.ggp.utils.exploitability.ExploitabilityUtils;
import com.ggp.utils.strategy.NormalizingStrategyWrapper;
import picocli.CommandLine;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.ObjectInputStream;

@CommandLine.Command(name = "gp-to-csv",
        mixinStandardHelpOptions = true,
        description = "Convert results from game-playing evaluator to CSV",
        optionListHeading = "%nOptions:%n",
        sortOptions = false
)
public class GPToCSVCommand implements Runnable {
    @CommandLine.ParentCommand
    private MainCommand mainCommand;

    @CommandLine.Parameters(index = "0")
    private String resultsDir;

    private String getFileName(int initMs, int intendedTime, int gameCount, String dateKey) {
        return String.format("gp-%d-%d-%d-%s-%s.csv", initMs, intendedTime, gameCount, dateKey, "conv");
    }

    @Override
    public void run() {
        for (File gameDir: new File(resultsDir).listFiles()) {
            if (!gameDir.isDirectory()) continue;
            IGameDescription gameDesc = null;
            try {
                gameDesc = mainCommand.getConfigurableFactory().create(IGameDescription.class, ParseUtils.parseConfigExpression(gameDir.getName()));
            } catch (ConfigAssemblyException e) { }
            if (gameDesc == null) {
                System.out.println("Skipping " + gameDir.getName());
            }
            System.out.println("Checking " + gameDir.getName());
            for (File solverDir: gameDir.listFiles()) {
                if (!solverDir.isDirectory()) continue;
                System.out.println("\tChecking " + solverDir.getName());
                for (File gpentry: solverDir.listFiles()) {
                    if (!gpentry.isFile()) continue;
                    GamePlayingSaver.EntryMetadata meta = GamePlayingSaver.splitEntryFilename(gpentry.getName());
                    if (meta == null) continue;

                    try (FileInputStream fileInputStream = new FileInputStream(gpentry)) {
                        ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
                        EvaluatorEntry entry = (EvaluatorEntry) objectInputStream.readObject();
                        if (entry == null) throw new Exception("null");
                        String csvFilename = solverDir + "/" + getFileName(meta.initMs, (int) entry.getIntendedTimeMs(), meta.gameCount, meta.dateKey);
                        if (new File(csvFilename).exists()) {
                            System.out.println("\t\tAlready converted " + gpentry.getName());
                            continue;
                        }
                        double exp = ExploitabilityUtils.computeExploitability(new NormalizingStrategyWrapper(entry.getAggregatedStrat()), gameDesc);
                        CsvSaver saver = new CsvSaver(new FileWriter(csvFilename));
                        saver.add(entry, exp);
                        saver.close();
                        System.out.println("\t\tConverted " + gpentry.getName());
                    } catch (Exception e) {
                        System.out.println("\t\tSkipping " + gpentry.getName() + ": " + e.getMessage());
                    }
                }
            }
        }
    }
}

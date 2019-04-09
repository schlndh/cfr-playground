package com.ggp.cli;

import com.ggp.IGameDescription;
import com.ggp.IInfoSetStrategy;
import com.ggp.IInformationSet;
import com.ggp.IStrategy;
import com.ggp.parsers.ParseUtils;
import com.ggp.parsers.exceptions.ConfigAssemblyException;
import com.ggp.player_evaluators.EvaluatorEntry;
import com.ggp.player_evaluators.savers.CsvSaver;
import com.ggp.player_evaluators.savers.GamePlayingSaver;
import com.ggp.utils.exploitability.ExploitabilityUtils;
import com.ggp.utils.strategy.NormalizingStrategyWrapper;
import com.ggp.utils.strategy.RestrictedStrategy;
import com.ggp.utils.strategy.Strategy;
import com.ggp.utils.strategy.UniformISStrategy;
import picocli.CommandLine;

import java.io.*;
import java.util.HashMap;
import java.util.HashSet;

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

    @CommandLine.Option(names={"-i", "--intersect"}, description="Limit strategies for each game to the intersection of defined IS")
    private boolean intersect;

    private HashMap<File, EvaluatorEntry> cachedEntries = new HashMap<>();

    private String getFileName(int initMs, int intendedTime, int gameCount, String dateKey) {
        return String.format("gp-%d-%d-%d-%s-%s.csv", initMs, intendedTime, gameCount, dateKey, "conv");
    }

    private EvaluatorEntry doLoadEntry(File gpentry) {
        try (FileInputStream fileInputStream = new FileInputStream(gpentry)) {
            ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
            EvaluatorEntry entry = (EvaluatorEntry) objectInputStream.readObject();
            if (entry == null) throw new Exception("null");
            return entry;
        } catch (Exception e) {
            System.out.println("\t\tSkipping " + gpentry.getName() + ": " + e.getMessage());
        }
        return null;
    }

    private EvaluatorEntry loadEntry(File gpentry) {
        if (intersect) {
            return cachedEntries.computeIfAbsent(gpentry, k -> doLoadEntry(gpentry));
        } else {
            return doLoadEntry(gpentry);
        }
    }

    private HashSet<IInformationSet> getStrategyIntersection(File gameDir) {
        HashSet<IInformationSet> intersection = null;
        if (!intersect) return null;
        for (File solverDir: gameDir.listFiles()) {
            if (!solverDir.isDirectory()) continue;
            for (File gpentry: solverDir.listFiles()) {
                if (!gpentry.isFile()) continue;
                GamePlayingSaver.EntryMetadata meta = GamePlayingSaver.splitEntryFilename(gpentry.getName());
                if (meta == null) continue;
                EvaluatorEntry entry = loadEntry(gpentry);
                if (entry == null) continue;
                Strategy strat = entry.getAggregatedStrat();
                if (intersection == null) {
                    intersection = new HashSet<>(strat.size());
                    for (IInformationSet is: strat.getDefinedInformationSets()) {
                        intersection.add(is);
                    }
                } else {
                    intersection.removeIf(is -> !strat.isDefined(is));
                }
            }
        }
        if (intersection != null) System.out.println("Limiting strategies to common " + intersection.size() + " IS.");
        return intersection;
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
            HashSet<IInformationSet> intersection = getStrategyIntersection(gameDir);
            for (File solverDir: gameDir.listFiles()) {
                if (!solverDir.isDirectory()) continue;
                System.out.println("\tChecking " + solverDir.getName());
                for (File gpentry: solverDir.listFiles()) {
                    if (!gpentry.isFile()) continue;
                    GamePlayingSaver.EntryMetadata meta = GamePlayingSaver.splitEntryFilename(gpentry.getName());
                    if (meta == null) continue;
                    EvaluatorEntry entry = loadEntry(gpentry);
                    if (entry == null) continue;
                    try {
                        String csvFilename = solverDir + "/" + getFileName(meta.initMs, (int) entry.getIntendedActTimeMs(), meta.gameCount, meta.dateKey);
                        if (new File(csvFilename).exists()) {
                            System.out.println("\t\tAlready converted " + gpentry.getName());
                            continue;
                        }
                        IStrategy strat = entry.getAggregatedStrat();
                        if (intersection != null) {
                            strat = new RestrictedStrategy(entry.getAggregatedStrat(), intersection);
                        }
                        double exp = ExploitabilityUtils.computeExploitability(new NormalizingStrategyWrapper(strat), gameDesc);
                        double firstActExp = ExploitabilityUtils.computeExploitability(new NormalizingStrategyWrapper(entry.getFirstActionStrat()), gameDesc);
                        CsvSaver saver = new CsvSaver(new FileWriter(csvFilename));
                        saver.add(entry, exp, firstActExp);
                        saver.close();
                        String stratSize = (intersection != null) ? (intersection.size() + "/" + entry.getAggregatedStrat().size())
                                : Integer.toString(entry.getAggregatedStrat().size());
                        System.out.println("\t\tConverted " + gpentry.getName() + ":\tIS " + stratSize + ", exp " + exp);
                    } catch (Exception e) {
                        System.out.println("\t\tSkipping " + gpentry.getName() + ": " + e.getMessage());
                    }
                }
            }
        }
    }
}

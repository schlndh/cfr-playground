package com.ggp.player_evaluators;

import com.ggp.*;
import com.ggp.player_evaluators.listeners.StrategyAggregatorListener;
import com.ggp.utils.exploitability.ExploitabilityUtils;
import com.ggp.utils.random.RandomSampler;
import com.ggp.utils.strategy.Strategy;
import com.ggp.utils.strategy.NormalizingStrategyWrapper;

import java.io.*;
import java.nio.file.Files;
import java.util.*;

/**
 * Evaluates Deepstack configuration by playing number of games against random opponent, while aggregating computed strategies
 * at encountered decision points.
 */
public class GamePlayingEvaluator implements IPlayerEvaluator {
    private static class Saver implements IPlayerEvaluationSaver {
        private String path;
        private int initMs;
        private String postfix;
        private int gameCount;

        public Saver(String path, int initMs, String postfix, int gameCount) {
            this.path = path;
            this.initMs = initMs;
            this.postfix = postfix;
            this.gameCount = gameCount;
        }

        private String getDateKey() {
            return String.format("%1$tY%1$tm%1$td-%1$tH%1$tM%1$tS", new Date());
        }

        private String getFileName(int intendedTime) {
            return String.format("gp-%d-%d-%d-%s-%s.gpentry", initMs, intendedTime, gameCount, getDateKey(), postfix.replace("-", ""));
        }

        @Override
        public void add(EvaluatorEntry e, double exploitability) throws IOException {
            String resFileName = path + "/" + getFileName((int) e.getIntendedTimeMs());
            FileOutputStream fileOutputStream = new FileOutputStream(resFileName);
            ObjectOutputStream objectOutputStream
                    = new ObjectOutputStream(fileOutputStream);
            objectOutputStream.writeObject(e);
            objectOutputStream.flush();
            objectOutputStream.close();
        }

        @Override
        public void close() throws IOException {
        }
    }

    private static class EntryMetadata {
        public final int initMs, intendedTime, gameCount;
        public final String dateKey, postfix;

        public EntryMetadata(int initMs, int intendedTime, int gameCount, String dateKey, String postfix) {
            this.initMs = initMs;
            this.intendedTime = intendedTime;
            this.gameCount = gameCount;
            this.dateKey = dateKey;
            this.postfix = postfix;
        }
    }

    private static EntryMetadata splitEntryFilename(String s) {
        final String EXT = ".gpentry";
        if (s.length() <= EXT.length()) return null;
        String name = s.substring(0, s.length() - EXT.length());
        String fileExt = s.substring(name.length());
        if (!EXT.equals(fileExt)) return null;
        String[] parts = name.split("-");
        if (parts.length != 7 || !"gp".equals(parts[0])) return null;
        try {
            return new EntryMetadata(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3]), parts[4] + "-" + parts[5], parts[6]);
        } catch (Exception e) {
            return null;
        }
    }

    public static void mergeSavedEntryFiles(String path, boolean dryRun) {
        // initMs -> intentedTime -> file
        HashMap<Integer, HashMap<Integer, ArrayList<File>>> grouppedEntries = new HashMap<>();
        for (File entry: new File(path).listFiles()) {
            if (!entry.isFile()) continue;
            EntryMetadata meta =  splitEntryFilename(entry.getName());
            if (meta == null) continue;
            grouppedEntries.computeIfAbsent(meta.initMs, k -> new HashMap<>())
                    .computeIfAbsent(meta.intendedTime, k -> new ArrayList<>())
                    .add(entry);
        }
        for (int initMs: grouppedEntries.keySet()) {
            HashMap<Integer, ArrayList<File>> intentedTimeFiles = grouppedEntries.get(initMs);
            for (int intendedTime: intentedTimeFiles.keySet()) {
                ArrayList<File> filesToGroup = intentedTimeFiles.get(intendedTime);
                EvaluatorEntry firstEntry = null;
                ArrayList<String> mergedFiles = new ArrayList<>(filesToGroup.size());
                int gameCount = 0;
                while (firstEntry == null && filesToGroup.size() > 1) {
                    File file = filesToGroup.get(0);
                    try (FileInputStream fileInputStream = new FileInputStream(file)) {
                        ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
                        firstEntry = (EvaluatorEntry) objectInputStream.readObject();
                        if (firstEntry == null) throw new Exception("null");
                        mergedFiles.add(file.getPath());
                        gameCount += splitEntryFilename(file.getName()).gameCount;
                    } catch (Exception e) {
                        System.out.println("\t\tSkipping " + file + ": " + e.getMessage());
                        filesToGroup.remove(0);
                    }
                }

                if (firstEntry == null) continue;

                for (int i = 1; i < filesToGroup.size(); ++i) {
                    File file = filesToGroup.get(i);
                    try (FileInputStream fileInputStream = new FileInputStream(file)) {
                        ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
                        EvaluatorEntry entry = (EvaluatorEntry) objectInputStream.readObject();
                        if (entry == null) throw new Exception("null");
                        firstEntry.merge(entry);
                        mergedFiles.add(file.getPath());
                        gameCount += splitEntryFilename(file.getName()).gameCount;
                    } catch (Exception e) {
                        System.out.println("\t\tSkipping " + file + ": " + e.getMessage());
                    }
                }
                {
                    System.out.println(String.format("\t\tMerging intended time %d: new gameCount = %d, defined IS = %d.", intendedTime, gameCount, firstEntry.getAggregatedStrat().size()));
                    for (String filename: mergedFiles) {
                        System.out.println("\t\t\t" + new File(filename).getName());
                    }
                    System.out.println();
                }

                if (!dryRun) {
                    Saver saver = new Saver(path, initMs, "merged", gameCount);
                    try {
                        saver.add(firstEntry, 0);
                        for (String filename: mergedFiles) {
                            File file = new File(filename);
                            System.out.println("\t\t\tDeleting " + file.getName());
                            file.delete();
                        }
                    } catch (IOException e) {
                        System.out.println(e);
                    }
                }
            }
        }
    }

    public static class Factory implements IFactory {
        private int gameCount;

        public Factory(int gameCount) {
            this.gameCount = gameCount;
        }

        @Override
        public IPlayerEvaluator create(int initMs, List<Integer> logPointsMs) {
            return new GamePlayingEvaluator(initMs, logPointsMs, gameCount);
        }

        @Override
        public String getConfigString() {
            return "GamePlayingEvaluator{" +
                    gameCount +
                    '}';
        }

        @Override
        public IPlayerEvaluationSaver createSaver(String path, int initMs, String postfix) throws IOException {
            return new Saver(path, initMs, postfix, gameCount);
        }
    }

    private int initMs;
    private int timeoutMs;
    StrategyAggregatorListener stratAggregator;
    private int gameCount;

    /**
     * Constructor
     * @param initMs timeout for deepstack initialization
     * @param logPointsMs ASC ordered list of times when strategies should be aggregated
     * @param gameCount number of games to play
     */
    public GamePlayingEvaluator(int initMs, List<Integer> logPointsMs, int gameCount) {
        this.initMs = initMs;
        this.timeoutMs = logPointsMs.get(logPointsMs.size() - 1);
        this.stratAggregator = new StrategyAggregatorListener(logPointsMs);
        this.gameCount = gameCount;
    }

    private void visit(HashSet<IInformationSet> infoSets, ICompleteInformationState s) {
        if (s.isTerminal()) {
            return;
        }
        if (s.isRandomNode()) {
            for (IAction a: s.getLegalActions()) {
                visit(infoSets, s.next(a));
            }
        } else {
            infoSets.add(s.getInfoSetForActingPlayer());
            for (IAction a: s.getLegalActions()) {
                visit(infoSets, s.next(a));
            }
        }
    }

    private int countInfoSets(IGameDescription gameDesc) {
        HashSet<IInformationSet> infoSets = new HashSet<>();
        visit(infoSets, gameDesc.getInitialState());
        return infoSets.size();
    }

    private static class TerminalAvoidingRandomPlayer implements IPlayer {
        public static class Factory implements IPlayerFactory {
            @Override
            public IPlayer create(IGameDescription game, int role) {
                return new TerminalAvoidingRandomPlayer(role);
            }

            @Override
            public String getConfigString() {
                return "TerminalAvoidingRandomPlayer{}";
            }
        }
        private int role;
        private ICompleteInformationState state;
        private RandomSampler sampler = new RandomSampler();

        public TerminalAvoidingRandomPlayer(int role) {
            this.role = role;
        }

        @Override
        public void init(long timeoutMillis) {
        }

        @Override
        public IAction act(long timeoutMillis) {
            List<IAction> actions = new ArrayList<>();
            for (IAction a: state.getLegalActions()) {
                if (!state.next(a).isTerminal()) actions.add(a);
            }
            if (actions.isEmpty()) {
                actions = state.getLegalActions();
            }
            return sampler.select(actions);
        }

        @Override
        public void forceAction(IAction a, long timeoutMillis) {
        }

        @Override
        public int getRole() {
            return role;
        }

        @Override
        public void receivePercepts(IPercept percept) {
        }

        public void setState(ICompleteInformationState state) {
            this.state = state;
        }
    }

    private static class TerminalAvoidingGameListener implements IGameListener {
        private TerminalAvoidingRandomPlayer player;

        @Override
        public void gameStart(IPlayer player1, IPlayer player2) {
            if (player1.getClass() == TerminalAvoidingRandomPlayer.class) {
                player = (TerminalAvoidingRandomPlayer) player1;
            } else  {
                player = (TerminalAvoidingRandomPlayer) player2;
            }
        }

        @Override
        public void gameEnd(int payoff1, int payoff2) {
        }

        @Override
        public void stateReached(ICompleteInformationState s) {
            if (player != null) player.setState(s);
        }

        @Override
        public void actionSelected(ICompleteInformationState s, IAction a) {
        }
    }

    @Override
    public List<EvaluatorEntry> evaluate(IGameDescription gameDesc, IEvaluablePlayer.IFactory playerFactory, boolean quiet) {
        IPlayerFactory random = new TerminalAvoidingRandomPlayer.Factory();
        long lastVisitedStates[] = new long[stratAggregator.getEntries().size()];
        playerFactory.registerResolvingListener(stratAggregator);
        final int evalEach = gameCount/Math.min(gameCount, 10);
        int evals = 0;
        final int totalInfoSets = countInfoSets(gameDesc);
        for (int i = 0; i < gameCount; ++i) {
            IPlayerFactory pl1 = playerFactory, pl2 = random;
            if (i % 2 == 1) {
                pl1 = random;
                pl2 = playerFactory;
            }
            stratAggregator.reinit();
            GameManager manager = new GameManager(pl1, pl2, gameDesc);
            manager.registerGameListener(new TerminalAvoidingGameListener());
            manager.run(initMs, timeoutMs);
            List<EvaluatorEntry> entries = stratAggregator.getEntries();
            for (int j = 0; j < entries.size(); ++j) {
                EvaluatorEntry e = entries.get(j);
                e.addPathStates(e.getAvgVisitedStates() - lastVisitedStates[j], 1);
                lastVisitedStates[j] = e.getAvgVisitedStates();
            }
            if (!quiet) {
                EvaluatorEntry lastEntry = entries.get(entries.size() - 1);
                Strategy strat = lastEntry.getAggregatedStrat();
                if ((i+1) == evalEach*(evals + 1)) {
                    double exp = ExploitabilityUtils.computeExploitability(new NormalizingStrategyWrapper(strat), gameDesc);
                    System.out.println(String.format("\r[%4d]: (%d ms, %d avg. states, %d/%d IS defined) -> %.4f exp", i+1, (int) lastEntry.getEntryTimeMs(), lastEntry.getAvgVisitedStates()/(i+1), strat.size(), totalInfoSets, exp));
                    evals++;
                } else {
                    System.out.print(String.format("\r[%4d]: (%d ms, %d avg. states, %d/%d IS defined)", i+1, (int) lastEntry.getEntryTimeMs(), lastEntry.getAvgVisitedStates()/(i+1), strat.size(), totalInfoSets));
                }
            }

        }

        for (EvaluatorEntry entry: stratAggregator.getEntries()) {
            entry.getAggregatedStrat().normalize();
            entry.setVisitedStatesNorm(gameCount);
        }
        return stratAggregator.getEntries();
    }
}

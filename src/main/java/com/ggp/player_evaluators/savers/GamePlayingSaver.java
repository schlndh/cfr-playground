package com.ggp.player_evaluators.savers;

import com.ggp.player_evaluators.EvaluatorEntry;
import com.ggp.player_evaluators.IPlayerEvaluationSaver;

import java.io.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

public class GamePlayingSaver implements IPlayerEvaluationSaver {
    private String path;
    private int initMs;
    private String postfix;
    private int gameCount;

    public GamePlayingSaver(String path, int initMs, String postfix, int gameCount) {
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
    public void add(EvaluatorEntry e, double exploitability, double firstActExp) throws IOException {
        String resFileName = path + "/" + getFileName((int) e.getIntendedActTimeMs());
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

    public static class EntryMetadata {
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

    public static EntryMetadata splitEntryFilename(String s) {
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
                    GamePlayingSaver saver = new GamePlayingSaver(path, initMs, "merged", gameCount);
                    try {
                        saver.add(firstEntry, 0, 0);
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
}
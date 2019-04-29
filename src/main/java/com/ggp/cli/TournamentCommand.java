package com.ggp.cli;

import com.ggp.*;
import com.ggp.utils.PlayerHelpers;
import com.ggp.utils.time.StopWatch;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import picocli.CommandLine;

import java.io.*;
import java.util.Date;

@CommandLine.Command(name = "tournament",
        mixinStandardHelpOptions = true,
        description = "Runs tournament of given players in given game",
        optionListHeading = "%nOptions:%n",
        sortOptions = false
)
public class TournamentCommand implements Runnable {
    @CommandLine.ParentCommand
    private MainCommand mainCommand;

    @CommandLine.Option(names={"-g", "--game"}, description="Game to be played", required=true)
    private IGameDescription game;

    @CommandLine.Option(names={"--player1"}, description="Player 1", required=true)
    private IPlayerFactory player1;

    @CommandLine.Option(names={"--player2"}, description="Player 2", required=true)
    private IPlayerFactory player2;

    @CommandLine.Option(names={"-i", "--init"}, description="Init time (ms)", required=true)
    private int init;

    @CommandLine.Option(names={"-t", "--time-limit"}, description="Time limit per move (ms)", required=true)
    private int timeLimit;

    @CommandLine.Option(names={"-c", "--count"}, description="How many games to play", defaultValue="1")
    private int count;

    @CommandLine.Option(names={"-d", "--dry-run"}, description="Dry run - doesn't save output")
    private boolean dryRun;

    @CommandLine.Option(names={"-q", "--quiet"}, description="Quiet mode - doesn't print output")
    private boolean quiet;

    @CommandLine.Option(names={"--fixed-roles"}, description="Fix player roles (player roles are swapped after each game by default)")
    private boolean fixedRoles;

    @CommandLine.Option(names={"--res-dir"}, description="Results directory", defaultValue="tournament-results")
    private String resultsDirectory;

    @CommandLine.Option(names={"--res-postfix"}, description="Postfix for result files", defaultValue="0")
    private String resultPostfix;

    @CommandLine.Option(names={"--skip-warmup"}, description="Skip warm-up")
    private boolean skipWarmup;

    private String getDateKey() {
        return String.format("%1$tY%1$tm%1$td-%1$tH%1$tM%1$tS", new Date());
    }

    private String getCSVName() {
        return String.format("%d-%d-%s-%s.csv", init, timeLimit, getDateKey(), resultPostfix.replace("-", ""));
    }

    private static class MatchData {
        public StopWatch[] initTimers = new StopWatch[] {new StopWatch(), new StopWatch(), new StopWatch()};
        public StopWatch[] actTimers = new StopWatch[] {new StopWatch(), new StopWatch(), new StopWatch()};
        public int[] playerActions = new int[] {0,0,0};
        public double[] payoff = new double[] {0,0,0};
    }

    private void warmup() {
        if (skipWarmup) return;
        if (!quiet) System.out.println("Warming up...");
        StopWatch timer = new StopWatch();
        timer.start();
        do {
            runGame(player1, player2, 1000, 250);
        } while (timer.getLiveDurationMs() < 30000);
        if (!quiet) System.out.println(String.format("Warm-up complete in %dms.", timer.getLiveDurationMs()));
    }

    private MatchData runGame(IPlayerFactory pl1, IPlayerFactory pl2, int initTimeMs, int timeLimitMs) {
        MatchData ret = new MatchData();
        GameManager manager = new GameManager(pl1, pl2, game);
        manager.registerGameListener(new IGameListener() {
            @Override
            public void playerInitStarted(int player) {
                ret.initTimers[player].start();
            }

            @Override
            public void playerInitFinished(int player) {
                ret.initTimers[player].stop();
            }

            @Override
            public void gameStart(IPlayer player1, IPlayer player2) {
            }

            @Override
            public void gameEnd(int payoff1, int payoff2) {
                ret.payoff[1] = payoff1;
                ret.payoff[2] = payoff2;
            }

            @Override
            public void stateReached(ICompleteInformationState s) {
                if (s.isTerminal()) return;
                ret.actTimers[s.getActingPlayerId()].start();
            }

            @Override
            public void actionSelected(ICompleteInformationState s, IAction a) {
                ret.actTimers[s.getActingPlayerId()].stop();
                ret.playerActions[s.getActingPlayerId()]++;
            }
        });
        manager.run(initTimeMs, timeLimitMs);
        return ret;
    }

    @Override
    public void run() {
        if (game == null) {
            System.err.println("Game can't be null!");
            return;
        }
        if (player1 == null) {
            System.err.println("Player 1 can't be null!");
            return;
        }
        if (player2 == null) {
            System.err.println("Player 2 can't be null!");
            return;
        }
        String gameDir = resultsDirectory + "/" + game.getConfigString();
        String fileName = gameDir + "/" + getCSVName();
        if (!dryRun) new File(gameDir).mkdirs();

        if (!quiet) {
            if (dryRun) {
                System.out.println(String.format("Tournament of %s vs %s in %s.", player1.getConfigString(), player2.getConfigString(), game.getConfigString()));
            } else {
                System.out.println(String.format("Tournament of %s vs %s in %s logged to %s.", player1.getConfigString(), player2.getConfigString(), game.getConfigString(), fileName));
            }
        }
        warmup();
        int countDigits = (int) Math.ceil(Math.log10(count));
        double[] totalPayoff = new double[]{0,0,0};
        try {
            Writer output = dryRun ? new StringWriter() : new FileWriter(fileName);
            CSVPrinter csvOut = new CSVPrinter(output,
                    CSVFormat.EXCEL.withHeader("player1", "player2", "intended_init_time", "intended_time", "init1", "init2",
                            "time_sum1", "time_sum2", "actions1", "actions2", "payoff1", "payoff2"));
            for (int matchId = 0; matchId < count; ++matchId) {
                boolean swapPlayers = !fixedRoles && matchId % 2 == 1;
                IPlayerFactory pl1, pl2;
                if (!swapPlayers) {
                    pl1 = player1;
                    pl2 = player2;
                } else {
                    pl1 = player2;
                    pl2 = player1;
                }
                MatchData data = runGame(pl1, pl2, init, timeLimit);
                csvOut.printRecord(pl1.getConfigString(), pl2.getConfigString(), init, timeLimit,
                        data.initTimers[1].getDurationMs(), data.initTimers[2].getDurationMs(),
                        data.actTimers[1].getDurationMs(), data.actTimers[2].getDurationMs(),
                        data.playerActions[1], data.playerActions[2], data.payoff[1], data.payoff[2]);
                csvOut.flush();
                int swId1 = swapPlayers ? 2 : 1;
                int swId2 = PlayerHelpers.getOpponentId(swId1);
                String status = String.format("[Game %" + countDigits +"d]: init (%d, %d), act (%d, %d) -> payoff (%.4f, %.4f)",
                        matchId + 1, data.initTimers[swId1].getDurationMs(), data.initTimers[swId2].getDurationMs(),
                        data.actTimers[swId1].getDurationMs()/Math.max(data.playerActions[swId1], 1), data.actTimers[swId2].getDurationMs()/Math.max(data.playerActions[swId2], 1),
                        data.payoff[swId1], data.payoff[swId2]);
                if (!quiet) {
                    System.out.println(status);
                }
                totalPayoff[1] += data.payoff[swId1];
                totalPayoff[2] += data.payoff[swId2];
            }
            if (!quiet) {
                System.out.println(String.format("Average payoffs: %.4f, %.4f", totalPayoff[1]/count, totalPayoff[2]/count));
            }
        } catch (IOException e) {
            System.err.println(e.getMessage());
            return;
        }

    }
}

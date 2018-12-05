package com.ggp.benchmark;

public class BenchmarkResultEntry {
    private final double timeMs;
    private final long iterations;
    private final long statesVisited;
    private final double exploitability;
    private final double avgRegret;


    public BenchmarkResultEntry(double timeMs, long iterations, long statesVisited, double exploitability, double avgRegret) {
        this.timeMs = timeMs;
        this.iterations = iterations;
        this.statesVisited = statesVisited;
        this.exploitability = exploitability;
        this.avgRegret = avgRegret;
    }

    public double getTimeMs() {
        return timeMs;
    }

    public long getIterations() {
        return iterations;
    }

    public long getStatesVisited() {
        return statesVisited;
    }

    public double getExploitability() {
        return exploitability;
    }

    public double getAvgRegret() {
        return avgRegret;
    }
}
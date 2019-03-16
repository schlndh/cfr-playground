package com.ggp.utils.time;

public class IterationTimer {
    private StopWatch totalTime = new StopWatch();
    private StopWatch iterTime = new StopWatch();
    private boolean iterRunning = false;
    private long timeoutMs;
    private long iters = 0;
    private long totalIterTime = 0;

    public IterationTimer(long timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    public void start() {
        totalTime.start();
        if (iterRunning) iterTime.start();
    }

    public void stop() {
        totalTime.stop();
        if (iterRunning) iterTime.start();
    }

    public void startIteration() {
        iterTime.reset();
        iterRunning = true;
    }

    public void endIteration() {
        iterTime.stop();
        iterRunning = false;
        totalIterTime += iterTime.getDurationMs();
        iters++;
    }

    private long getEstimatedIterationLengthMs() {
        if (iters == 0) return 0;
        return totalIterTime/iters;
    }

    public boolean canDoAnotherIteration() {
        long remainingMs = timeoutMs - totalTime.getLiveDurationMs();
        return (remainingMs > getEstimatedIterationLengthMs());
    }

    public long getLiveTotalDurationMs() {
        return totalTime.getLiveDurationMs();
    }

    public long getLiveIterDurationMs() {
        return iterTime.getLiveDurationMs();
    }
}

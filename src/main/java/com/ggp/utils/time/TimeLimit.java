package com.ggp.utils.time;

public class TimeLimit {
    private StopWatch timer = new StopWatch();
    private long timeLimitMs;

    public TimeLimit(long timeLimitMs) {
        this.timeLimitMs = timeLimitMs;
    }

    public void start() {
        timer.start();
    }

    public boolean isFinished() {
        return timer.getLiveDurationMs() > timeLimitMs;
    }
}

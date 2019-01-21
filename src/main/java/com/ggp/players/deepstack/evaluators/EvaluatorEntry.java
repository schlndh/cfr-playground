package com.ggp.players.deepstack.evaluators;

import com.ggp.utils.strategy.Strategy;

public class EvaluatorEntry {
    private double intendedTimeMs;
    private double avgTimeMs = 0;
    private double avgTimeWeigthNorm = 0;
    private long visitedStates = 0;
    private long visitedStatesNorm = 1;
    private Strategy aggregatedStrat = new Strategy();

    public EvaluatorEntry(double intendedTimeMs) {
        this.intendedTimeMs = intendedTimeMs;
    }

    public EvaluatorEntry(double intendedTimeMs, double avgTimeMs, Strategy aggregatedStrat) {
        this.intendedTimeMs = intendedTimeMs;
        this.avgTimeMs = avgTimeMs;
        this.avgTimeWeigthNorm = 1;
        this.aggregatedStrat = aggregatedStrat;
    }

    public void addTime(double time, double weight) {
        avgTimeMs += time*weight;
        avgTimeWeigthNorm += weight;
    }

    public void addVisitedStates(long states) {
        visitedStates += states;
    }

    public double getIntendedTimeMs() {
        return intendedTimeMs;
    }

    public double getEntryTimeMs() {
        return avgTimeMs/avgTimeWeigthNorm;
    }

    public Strategy getAggregatedStrat() {
        return aggregatedStrat;
    }

    public long getAvgVisitedStates() {
        return visitedStates / visitedStatesNorm;
    }

    public void setVisitedStatesNorm(long visitedStatesNorm) {
        this.visitedStatesNorm = visitedStatesNorm;
    }
}

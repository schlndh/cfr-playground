package com.ggp.player_evaluators;

import com.ggp.utils.strategy.Strategy;

import java.io.Serializable;

public class EvaluatorEntry implements Serializable {
    private static final long serialVersionUID = 1L;
    private double intendedTimeMs;
    private double avgTimeMs = 0;
    private double avgTimeWeigthNorm = 0;
    private long initVisitedStates = 0;
    private long visitedStates = 0;
    private long visitedStatesNorm = 1;
    private long pathStatesMin = Long.MAX_VALUE;
    private long pathStatesMax = 0;
    private long pathStatesSum = 0;
    private double pathStatesNorm = 0;
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

    public void addInitVisitedStates(long states) {
        initVisitedStates += states;
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

    public long getAvgInitVisitedStates() {
        return initVisitedStates / visitedStatesNorm;
    }

    public void setVisitedStatesNorm(long visitedStatesNorm) {
        this.visitedStatesNorm = visitedStatesNorm;
    }

    public long getPathStatesMin() {
        return pathStatesMin;
    }

    public long getPathStatesMax() {
        return pathStatesMax;
    }

    public long getPathStatesAvg() {
        return (long) (pathStatesSum / pathStatesNorm);
    }

    public void addPathStates(long pathStates, double weight) {
        if (pathStatesMin > pathStates) pathStatesMin = pathStates;
        else if (pathStatesMax < pathStates) pathStatesMax = pathStates;
        pathStatesSum += weight * pathStates;
        pathStatesNorm += weight;
    }
}

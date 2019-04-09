package com.ggp.player_evaluators;

import com.ggp.utils.strategy.Strategy;

import java.io.Serializable;

public class EvaluatorEntry implements Serializable {
    private static final long serialVersionUID = 1L;
    private double intendedActTimeMs;
    private double avgActTimeMs = 0;
    private double avgActTimeWeigthNorm = 0;
    private double intendedInitTimeMs = 0;
    private double initTimeMsSum = 0;
    private double initTimeNorm = 0;
    private long initVisitedStates = 0;
    private long visitedStates = 0;
    private long visitedStatesNorm = 1;
    private long pathStatesMin = Long.MAX_VALUE;
    private long pathStatesMax = 0;
    private long pathStatesSum = 0;
    private double pathStatesNorm = 0;
    private Strategy aggregatedStrat = new Strategy();
    private Strategy firstActionStrat = new Strategy();

    public EvaluatorEntry(double intendedInitTimeMs, double intendedActTimeMs) {
        this.intendedInitTimeMs = intendedInitTimeMs;
        this.intendedActTimeMs = intendedActTimeMs;
    }

    public EvaluatorEntry(double intendedActTimeMs, double avgActTimeMs, Strategy aggregatedStrat) {
        this.intendedActTimeMs = intendedActTimeMs;
        this.avgActTimeMs = avgActTimeMs;
        this.avgActTimeWeigthNorm = 1;
        this.aggregatedStrat = aggregatedStrat;
    }

    public void addTime(double time, double weight) {
        avgActTimeMs += time*weight;
        avgActTimeWeigthNorm += weight;
    }

    public void addVisitedStates(long states) {
        visitedStates += states;
    }

    public void addInitVisitedStates(long states) {
        initVisitedStates += states;
    }

    public double getIntendedActTimeMs() {
        return intendedActTimeMs;
    }

    public double getEntryTimeMs() {
        return avgActTimeMs / avgActTimeWeigthNorm;
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

    public double getIntendedInitTimeMs() {
        return intendedInitTimeMs;
    }

    public double getAvgInitTimeMs() {
        if (initTimeNorm == 0) return 0;
        return initTimeMsSum/initTimeNorm;
    }

    public void addInitTime(double time) {
        initTimeMsSum += time;
        initTimeNorm += 1;
    }

    public Strategy getFirstActionStrat() {
        return firstActionStrat;
    }

    public void merge(EvaluatorEntry other) {
        if (intendedActTimeMs != other.getIntendedActTimeMs() || intendedInitTimeMs != other.getIntendedInitTimeMs()) throw new RuntimeException("Can't merge evaluator entries with different intended times.");
        this.avgActTimeMs += other.avgActTimeMs;
        this.avgActTimeWeigthNorm += other.avgActTimeWeigthNorm;
        this.initTimeMsSum += other.initTimeMsSum;
        this.initTimeNorm += other.initTimeNorm;
        this.initVisitedStates += other.initVisitedStates;
        this.visitedStates += other.visitedStates;
        this.visitedStatesNorm += other.visitedStatesNorm;
        this.pathStatesMin = Math.min(this.pathStatesMin, other.pathStatesMin);
        this.pathStatesMax = Math.max(this.pathStatesMax, other.pathStatesMax);
        this.pathStatesSum += other.pathStatesSum;
        this.pathStatesNorm += other.pathStatesNorm;
        this.aggregatedStrat.merge(other.aggregatedStrat);
        this.firstActionStrat.merge(other.firstActionStrat);
    }
}

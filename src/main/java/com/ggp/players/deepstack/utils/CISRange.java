package com.ggp.players.deepstack.utils;

import com.ggp.IAction;
import com.ggp.ICompleteInformationState;
import com.ggp.IInformationSet;
import com.ggp.IStrategy;
import com.ggp.IInfoSetStrategy;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CISRange implements Serializable {
    private HashMap<ICompleteInformationState, Double> range = new HashMap<>();
    private double norm = 1d;

    public CISRange(ICompleteInformationState initialState) {
        range.put(initialState, 1d);
    }

    public CISRange(Set<ICompleteInformationState> subgameStates, NextRangeTree nrt, IStrategy lastCumulativeStrategy) {
        norm = 0;
        for (ICompleteInformationState s: subgameStates) {
            double stateReachProb = 0;
            for (Map.Entry<IInformationSet, ? extends Map<IAction, Double>> paths: nrt.getNextTurnStatePaths(s).entrySet()) {
                IInformationSet origIs = paths.getKey();
                IInfoSetStrategy origIsStrat = (origIs == null) ? null : lastCumulativeStrategy.getInfoSetStrategy(origIs);
                List<IAction> legalActions = (origIs == null) ? null : origIs.getLegalActions();
                for (Map.Entry<IAction, Double> actionToRndProb: paths.getValue().entrySet()) {
                    double pathProb = actionToRndProb.getValue();
                    IAction a = actionToRndProb.getKey();
                    if (a != null) {
                        int actionIdx = legalActions.indexOf(a);
                        double actionProb = origIsStrat.getProbability(actionIdx);
                        pathProb *= actionProb;
                    }

                    stateReachProb += pathProb;
                }
            }
            range.put(s, stateReachProb);
            norm += stateReachProb;
        }
        if (norm == 0) {
            norm = 1;
            double prob = 1d / range.size();
            range.replaceAll((k, v) -> prob);
        }

    }

    public double getProbability(ICompleteInformationState s) {
        return range.getOrDefault(s, 0d);
    }

    public Set<ICompleteInformationState> getPossibleStates() {
        return range.keySet();
    }

    public int size() {
        return range.size();
    }

    public Set<?extends Map.Entry<ICompleteInformationState, Double>> getProbabilities() {
        return range.entrySet();
    }

    public double getNorm() {
        return norm;
    }
}

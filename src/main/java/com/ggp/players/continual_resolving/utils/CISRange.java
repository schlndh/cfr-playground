package com.ggp.players.continual_resolving.utils;

import com.ggp.ICompleteInformationState;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class CISRange implements Serializable {
    private HashMap<ICompleteInformationState, Double> range = new HashMap<>();
    private double norm = 1d;

    public CISRange(ICompleteInformationState initialState) {
        range.put(initialState, 1d);
    }

    public CISRange(Set<ICompleteInformationState> subgameStates, Map<ICompleteInformationState, Double> reachProbs, double reachProbNorm) {
        norm = 0;
        for (ICompleteInformationState s: subgameStates) {
            double stateReachProb = reachProbs.get(s)/reachProbNorm;
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

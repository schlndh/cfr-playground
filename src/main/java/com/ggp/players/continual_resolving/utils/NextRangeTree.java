package com.ggp.players.continual_resolving.utils;

import com.ggp.IAction;
import com.ggp.ICompleteInformationState;
import com.ggp.IInformationSet;

import java.util.HashMap;
import java.util.Map;

public class NextRangeTree {
    private HashMap<ICompleteInformationState, HashMap<IInformationSet, HashMap<IAction, Double>>> rangeMap = new HashMap<>();

    public void add(ICompleteInformationState nextTurnState, IInformationSet origIs, IAction myAction, double rndProb) {
        HashMap<IInformationSet, HashMap<IAction, Double>> pathsToState = rangeMap.computeIfAbsent(nextTurnState, k -> new HashMap<>());
        HashMap<IAction, Double> probMap = pathsToState.computeIfAbsent(origIs, k -> new HashMap<>());
        probMap.merge(myAction, rndProb, (oldV, newV) -> oldV + newV);
    }

    public Map<IInformationSet, ?extends Map<IAction, Double>> getNextTurnStatePaths(ICompleteInformationState s) {
        return rangeMap.getOrDefault(s, null);
    }
}

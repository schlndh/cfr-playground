package com.ggp.utils;

import com.ggp.IAction;
import com.ggp.IInfoSetStrategy;
import com.ggp.IInformationSet;
import com.ggp.IStrategy;

import java.util.List;

public class Metrics {
    public static double getStrategyMSE(IStrategy normalizedTarget, IStrategy unnormalizedCurrent, IInformationSet is) {
        IInfoSetStrategy targetIsStrat = normalizedTarget.getInfoSetStrategy(is);
        IInfoSetStrategy currentIsStrat = unnormalizedCurrent.getInfoSetStrategy(is);
        double errSum = 0;
        List<IAction> legalActions = is.getLegalActions();
        if (legalActions == null || legalActions.isEmpty()) return errSum;
        double total = 0;
        int actionIdx = 0;
        for (IAction a: legalActions) {
            total += currentIsStrat.getProbability(actionIdx);
            actionIdx++;
        }
        actionIdx = 0;
        for (IAction a: legalActions) {
            double diff = targetIsStrat.getProbability(actionIdx);
            if (total > 0) {
                diff -= currentIsStrat.getProbability(actionIdx)/total;
            } else {
                diff -= 1d/legalActions.size();
            }
            errSum += diff*diff;
            actionIdx++;
        }
        return errSum;
    }

    public static double getStrategyMSE(IStrategy normalizedTarget, IStrategy unnormalizedCurrent) {
        int isCount = 0;
        double errSum = 0;
        for (IInformationSet is: unnormalizedCurrent.getDefinedInformationSets()) {
            isCount++;
            errSum += getStrategyMSE(normalizedTarget, unnormalizedCurrent, is);
        }
        if (isCount > 0) return errSum/isCount;
        return 0;
    }
}

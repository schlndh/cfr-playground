package com.ggp.players.deepstack.regret_matching;

import com.ggp.IInformationSet;
import com.ggp.players.deepstack.IRegretMatching;
import com.ggp.utils.strategy.InfoSetStrategy;
import com.ggp.utils.strategy.Strategy;

import java.util.HashMap;

abstract class BaseRegretMatching implements IRegretMatching {
    protected HashMap<IInformationSet, double[]> regrets = new HashMap<>();
    protected double totalRegret = 0;

    private double[] getOrCreateActionRegrets(IInformationSet is) {
        return regrets.computeIfAbsent(is, k -> new double[is.getLegalActions().size()]);
    }

    protected abstract double sumRegrets(double r1, double r2);

    @Override
    public void addActionRegret(IInformationSet is, int actionIdx, double regretDiff) {
        double[] actionRegrets = getOrCreateActionRegrets(is);
        totalRegret -= Math.max(0, actionRegrets[actionIdx]);
        actionRegrets[actionIdx] = sumRegrets(actionRegrets[actionIdx], regretDiff);
        totalRegret += Math.max(0, actionRegrets[actionIdx]);
    }

    @Override
    public boolean hasInfoSet(IInformationSet is) {
        return regrets.containsKey(is);
    }

    @Override
    public void getRegretMatchedStrategy(IInformationSet is, Strategy strat) {
        double[] actionRegrets = getOrCreateActionRegrets(is);
        InfoSetStrategy isStrat = strat.getInfoSetStrategy(is);
        isStrat.setProbabilities(actionIdx -> Math.max(0, actionRegrets[actionIdx]));
        isStrat.normalize();
    }

    @Override
    public void getRegretMatchedStrategy(Strategy strat) {
        for (IInformationSet is: regrets.keySet()) {
            getRegretMatchedStrategy(is, strat);
        }
    }

    @Override
    public void initInfoSet(IInformationSet is) {
        getOrCreateActionRegrets(is);
    }

    @Override
    public double getTotalRegret() {
        return totalRegret;
    }
}

package com.ggp.solvers.cfr.regret_matching;

import com.ggp.solvers.cfr.IRegretMatching;
import com.ggp.utils.strategy.InfoSetStrategy;

import java.util.Arrays;

abstract class BaseRegretMatching implements IRegretMatching {
    protected double[] actionRegrets;


    protected BaseRegretMatching(BaseRegretMatching rm) {
        this.actionRegrets = Arrays.copyOf(rm.actionRegrets, rm.actionRegrets.length);
    }

    public BaseRegretMatching(int actionSize) {
        this.actionRegrets = new double[actionSize];
    }

    protected abstract double sumRegrets(double oldRegret, double regretDiff);

    @Override
    public void addActionRegret(int actionIdx, double regretDiff) {
        actionRegrets[actionIdx] = sumRegrets(actionRegrets[actionIdx], regretDiff);
    }

    @Override
    public void getRegretMatchedStrategy(double[] probabilities) {
        InfoSetStrategy isStrat = InfoSetStrategy.fromArrayReference(probabilities);
        isStrat.setProbabilities(actionIdx -> Math.max(0, actionRegrets[actionIdx]));
        isStrat.normalize();
    }

    @Override
    public double getRegret(int actionIdx) {
        return actionRegrets[actionIdx];
    }
}

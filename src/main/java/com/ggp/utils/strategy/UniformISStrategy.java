package com.ggp.utils.strategy;

import com.ggp.IInfoSetStrategy;

public class UniformISStrategy implements IInfoSetStrategy {
    private final int size;
    private final double prob;

    public UniformISStrategy(int size) {
        this.size = size;
        this.prob = 1d/size;
    }

    @Override
    public double getProbability(int actionIdx) {
        return prob;
    }

    @Override
    public int size() {
        return size;
    }
}

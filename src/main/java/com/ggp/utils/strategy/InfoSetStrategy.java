package com.ggp.utils.strategy;

import com.ggp.IInfoSetStrategy;
import com.ggp.IInformationSet;

import java.util.Arrays;
import java.util.function.Function;

public class InfoSetStrategy implements IInfoSetStrategy {
    private static final long serialVersionUID = 1L;
    private final double[] probabilities;


    public InfoSetStrategy(double[] probabilities) {
        this.probabilities = Arrays.copyOf(probabilities, probabilities.length);
    }

    public InfoSetStrategy(int actionSize) {
        this.probabilities = new double[actionSize];
        Arrays.fill(probabilities, 1d/actionSize);
    }

    public InfoSetStrategy(IInformationSet is) {
        this(is.getLegalActions().size());
    }

    @Override
    public double getProbability(int actionIdx) {
        if (actionIdx >= 0 && actionIdx < probabilities.length) return probabilities[actionIdx];
        return 0;
    }

    public void normalize() {
        double norm = 0;
        for (int i = 0; i < probabilities.length; ++i) {
            norm += probabilities[i];
        }
        if (norm == 0d) {
            for (int i = 0; i < probabilities.length; i++) {
                probabilities[i] = 1d/probabilities.length;
            }
        } else if (norm != 1d) {
            for (int i = 0; i < probabilities.length; i++) {
                probabilities[i] /= norm;
            }
        }
    }

    public InfoSetStrategy copy() {
        return new InfoSetStrategy(probabilities);
    }

    /**
     * Add values to probabilities. Doesn't normalize.
     * @param probMap
     */
    public void addProbabilities(Function<Integer, Double> probMap) {
        for (int i = 0; i < probabilities.length; ++i) {
            probabilities[i] += probMap.apply(i);
        }
    }

    /**
     * Override action probabilities. Doesn't normalize.
     * @param probMap
     */
    public void setProbabilities(Function<Integer, Double> probMap) {
        for (int i = 0; i < probabilities.length; ++i) {
            probabilities[i] = probMap.apply(i);
        }
    }

    @Override
    public int size() {
        return probabilities.length;
    }
}

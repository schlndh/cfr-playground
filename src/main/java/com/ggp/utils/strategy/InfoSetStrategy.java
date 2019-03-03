package com.ggp.utils.strategy;

import com.ggp.IInfoSetStrategy;
import com.ggp.IInformationSet;

import java.util.Arrays;
import java.util.function.Function;

public class InfoSetStrategy implements IInfoSetStrategy {
    private static final long serialVersionUID = 1L;
    private final double[] probabilities;


    private InfoSetStrategy(double[] probabilities) {
        this.probabilities = probabilities;
    }

    public InfoSetStrategy(int actionSize) {
        this.probabilities = new double[actionSize];
        Arrays.fill(probabilities, 1d/actionSize);
    }

    public InfoSetStrategy(IInformationSet is) {
        this(is.getLegalActions().size());
    }

    public static InfoSetStrategy fromArrayCopy(double[] probabilities) {
        return new InfoSetStrategy(Arrays.copyOf(probabilities, probabilities.length));
    }

    public static InfoSetStrategy fromArrayReference(double[] probabilites) {
        return new InfoSetStrategy(probabilites);
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
        return InfoSetStrategy.fromArrayCopy(probabilities);
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

    public void merge(InfoSetStrategy other) {
        if (other == null || other.probabilities.length != this.probabilities.length) throw new RuntimeException("Invalid InfoSetStrategy merge!");
        for (int i = 0; i < this.probabilities.length; ++i) {
            this.probabilities[i] += other.probabilities[i];
        }
    }
}

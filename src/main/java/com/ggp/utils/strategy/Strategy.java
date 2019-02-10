package com.ggp.utils.strategy;

import com.ggp.IInfoSetStrategy;
import com.ggp.IInformationSet;
import com.ggp.IStrategy;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class Strategy implements IStrategy {
    private static final long serialVersionUID = 2L;
    HashMap<IInformationSet, InfoSetStrategy> strategy = new HashMap<>();

    /**
     * Override action probabilities for given information set. Doesn't normalize;
     * @param is
     * @param probMap
     */
    public void setProbabilities(IInformationSet is, Function<Integer, Double> probMap) {
        getOrCreateISStrategy(is).setProbabilities(probMap);
    }

    public void addProbabilities(IInformationSet is, Function<Integer, Double> probMap) {
        if (isDefined(is)) {
            getOrCreateISStrategy(is).addProbabilities(probMap);
        } else {
            getOrCreateISStrategy(is).setProbabilities(probMap);
        }
    }

    public void normalize() {
        for(Map.Entry<IInformationSet, InfoSetStrategy> entry: strategy.entrySet()) {
            entry.getValue().normalize();
        }
    }

    @Override
    public Iterable<IInformationSet> getDefinedInformationSets() {
        return strategy.keySet();
    }

    public int countDefinedInformationSets() {
        return strategy.size();
    }

    public Strategy copy() {
        Strategy ret = new Strategy();
        for (Map.Entry<IInformationSet, InfoSetStrategy> kv: strategy.entrySet()) {
            ret.strategy.put(kv.getKey(), kv.getValue().copy());
        }
        return ret;
    }

    public boolean hasInformationSet(IInformationSet is) {
        return strategy.containsKey(is);
    }

    @Override
    public boolean isDefined(IInformationSet is) {
        return strategy.containsKey(is);
    }

    private InfoSetStrategy getOrCreateISStrategy(IInformationSet is) {
        if (is == null) return null;
        return strategy.computeIfAbsent(is, k -> new InfoSetStrategy(is.getLegalActions().size()));
    }

    @Override
    public IInfoSetStrategy getInfoSetStrategy(IInformationSet is) {
        if (is == null) return null;
        IInfoSetStrategy strat =  strategy.getOrDefault(is, null);
        if (strat == null) {
            return new UniformISStrategy(is.getLegalActions().size());
        }
        return strat;
    }

    public void setInfoSetStrategy(IInformationSet is, InfoSetStrategy isStrat) {
        strategy.put(is, isStrat);
    }
}

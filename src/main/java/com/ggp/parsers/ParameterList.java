package com.ggp.parsers;

import java.util.*;
import java.util.function.BiFunction;

public class ParameterList {
    private List<Parameter> positionalParams;
    private Map<String, Parameter> kvParams;
    private BiFunction<List<Object>, Map<String, Object>, Object> factory;

    public ParameterList(List<Parameter> positionalParams, Map<String, Parameter> kvParams, BiFunction<List<Object>, Map<String, Object>, Object> factory) {
        this.positionalParams = positionalParams == null ? null : Collections.unmodifiableList(new ArrayList<>(positionalParams)) ;
        this.kvParams = kvParams == null ? null : Collections.unmodifiableMap(new HashMap<>(kvParams));
        this.factory = factory;
    }

    public List<Parameter> getPositionalParams() {
        return positionalParams;
    }

    public Map<String, Parameter> getKvParams() {
        return kvParams;
    }

    public BiFunction<List<Object>, Map<String, Object>, Object> getFactory() {
        return factory;
    }
}

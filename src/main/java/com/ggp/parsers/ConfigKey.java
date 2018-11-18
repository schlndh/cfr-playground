package com.ggp.parsers;

import java.util.*;

public class ConfigKey {
    private String name;
    private ArrayList<ConfigExpression> positionalParams = new ArrayList<>();
    private HashMap<String, ConfigExpression> kvParams = new HashMap<>();

    public ConfigKey(String name) {
        this.name = name;
    }

    public void addPositionalParam(ConfigExpression param) {
        positionalParams.add(param);
    }

    public void addKVParam(String key, ConfigExpression value) {
        kvParams.put(key, value);
    }

    public String getName() {
        return name;
    }

    public List<ConfigExpression> getPositionalParams() {
        return positionalParams;
    }

    public Map<String, ConfigExpression> getKvParams() {
        return kvParams;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConfigKey configKey = (ConfigKey) o;
        return Objects.equals(name, configKey.name) &&
                Objects.equals(positionalParams, configKey.positionalParams) &&
                Objects.equals(kvParams, configKey.kvParams);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, positionalParams, kvParams);
    }
}

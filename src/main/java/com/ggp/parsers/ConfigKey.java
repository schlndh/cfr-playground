package com.ggp.parsers;

import java.util.*;

public class ConfigKey {
    private String name;
    private List<ConfigExpression> positionalParams;
    private Map<String, ConfigExpression> kvParams;

    public ConfigKey(String name, List<ConfigExpression> posParams, Map<String, ConfigExpression> kvParams) {
        this.name = name;
        this.positionalParams = posParams;
        this.kvParams = kvParams;
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

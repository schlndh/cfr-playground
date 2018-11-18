package com.ggp.parsers;

public class Parameter {
    private Class<?> type;
    private Object defaultValue;
    private boolean required;

    public Parameter(Class<?> type, Object defaultValue, boolean required) {
        this.type = type;
        this.defaultValue = defaultValue;
        this.required = required;
    }

    public Class<?> getType() {
        return type;
    }

    public Object getDefaultValue() {
        return defaultValue;
    }

    public boolean isRequired() {
        return required;
    }
}

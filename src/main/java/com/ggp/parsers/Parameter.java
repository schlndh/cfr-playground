package com.ggp.parsers;

/**
 * Parameter specification for a factory function.
 */
public class Parameter {
    private Class<?> type;
    private Object defaultValue;
    private boolean required;
    private String description;

    public Parameter(Class<?> type, Object defaultValue, boolean required) {
        this(type, defaultValue, required, "");
    }

    public Parameter(Class<?> type, Object defaultValue, boolean required, String description) {
        this.type = type;
        this.defaultValue = defaultValue;
        this.required = required;
        this.description = description;
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

    public String getDescription() {
        return description;
    }
}

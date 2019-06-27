package com.ggp.parsers;

import com.ggp.parsers.exceptions.ConfigAssemblyException;
import com.ggp.parsers.exceptions.WrongExpressionTypeException;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Parsed config expression. It consists of a type and a value.
 */
public class ConfigExpression {
    public enum Type {
        CONFIG_KEY, NUMBER, BOOL, STRING, ARRAY
    };

    private final Type type;
    private final ConfigKey configKey;
    private final String strValue;
    private final List<ConfigExpression> arrayValues;

    private ConfigExpression(Type type, ConfigKey configKey, String strValue, List<ConfigExpression> arrayValues) {
        this.type = type;
        this.configKey = configKey;
        this.strValue = strValue;
        this.arrayValues = arrayValues;
    }

    public static ConfigExpression createConfigKey(ConfigKey configKey) {
        return new ConfigExpression(Type.CONFIG_KEY, configKey, null, null);
    }

    public static ConfigExpression createArray(List<ConfigExpression> configExpressions) {
        return new ConfigExpression(Type.ARRAY, null, null, Collections.unmodifiableList(configExpressions));
    }

    public static ConfigExpression createValueType(Type type, String value) {
        return new ConfigExpression(type, null, value, null);
    }

    public static Type intToType(int type) {
        switch (type) {
            case ConfigKeyLexer.FLOAT:
            case ConfigKeyLexer.INT:
            case ConfigKeyLexer.INFINITY:
                return Type.NUMBER;
            case ConfigKeyLexer.BOOL:
                return Type.BOOL;
            case ConfigKeyLexer.STRING:
                return Type.STRING;
        }
        return Type.STRING;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConfigExpression that = (ConfigExpression) o;
        return type == that.type &&
                Objects.equals(configKey, that.configKey) &&
                Objects.equals(strValue, that.strValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, configKey, strValue);
    }

    public Type getType() {
        return type;
    }

    public boolean getBool() throws WrongExpressionTypeException {
        if (type != Type.BOOL) {
            throw new WrongExpressionTypeException();
        }
        return Boolean.valueOf(strValue);
    }

    public String getString() throws WrongExpressionTypeException {
        if (type != Type.STRING) {
            throw new WrongExpressionTypeException();
        }
        return strValue;
    }

    public int getInt() throws WrongExpressionTypeException {
        if (type != Type.NUMBER) {
            throw new WrongExpressionTypeException();
        }
        return Integer.valueOf(strValue);
    }

    public long getLong() throws WrongExpressionTypeException {
        if (type != Type.NUMBER) {
            throw new WrongExpressionTypeException();
        }
        return Long.valueOf(strValue);
    }

    public double getDouble() throws WrongExpressionTypeException {
        if (type != Type.NUMBER) {
            throw new WrongExpressionTypeException();
        }
        return Double.valueOf(strValue);
    }

    public ConfigKey getConfigKey() throws WrongExpressionTypeException {
        if (type != Type.CONFIG_KEY) {
            throw new WrongExpressionTypeException();
        }
        return configKey;
    }

    public List<ConfigExpression> getArrayValues() {
        return arrayValues;
    }

    /**
     * Get resulting array of primitive type (int, long, double, boolean and String)
     * @param arrType
     * @return
     * @throws WrongExpressionTypeException
     */
    public <T> T getPrimitiveArray(Class<T> arrType) throws WrongExpressionTypeException {
        if (type != Type.ARRAY) {
            throw new WrongExpressionTypeException();
        }
        try {
            return (new ConfigurableFactory()).create(arrType, this);
        } catch (ConfigAssemblyException e) {
            throw new WrongExpressionTypeException();
        }
    }
}

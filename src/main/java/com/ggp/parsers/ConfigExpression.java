package com.ggp.parsers;

import com.ggp.parsers.exceptions.WrongExpressionTypeException;

import java.util.Objects;

public class ConfigExpression {
    public enum Type {
        CONFIG_KEY, NUMBER, BOOL, STRING
    };

    private Type type;
    private ConfigKey configKey;
    private String strValue;

    private ConfigExpression(Type type, ConfigKey configKey, String strValue) {
        this.type = type;
        this.configKey = configKey;
        this.strValue = strValue;
    }

    public static ConfigExpression createConfigKey(ConfigKey configKey) {
        return new ConfigExpression(Type.CONFIG_KEY, configKey, null);
    }

    public static ConfigExpression createValueType(Type type, String value) {
        return new ConfigExpression(type, null, value);
    }

    public static Type intToType(int type) {
        switch (type) {
            case ConfigKeyLexer.FLOAT:
            case ConfigKeyLexer.INT:
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
}

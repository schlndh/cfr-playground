package com.ggp.parsers;

import com.ggp.parsers.exceptions.WrongConfigKeyException;
import com.ggp.parsers.exceptions.WrongExpressionTypeException;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfigurableFactory {
    private HashMap<Class<?>, HashMap<String, ArrayList<ParameterList>>> registry = new HashMap<>();

    public void register(Class<?> type, String name, ParameterList parameters) {
        registry.computeIfAbsent(type, a -> new HashMap<>())
                .computeIfAbsent(name, b -> new ArrayList<>())
                .add(parameters);
    }

    public static ParameterList createPositionalParameterList(Constructor<?> constructor) {
        ArrayList<Parameter> params = new ArrayList<>();
        for (Class<?> c: constructor.getParameterTypes()) {
            params.add(new Parameter(c, null, true));
        }
        return new ParameterList(params, null, (posParams, kvParams) -> {
            try {
                return constructor.newInstance(posParams.toArray());
            } catch (IllegalAccessException | InvocationTargetException | InstantiationException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public Object create(Class<?> type, ConfigKey key) throws WrongConfigKeyException {
        if (key == null) return null;
        HashMap<String, ArrayList<ParameterList>> typeRegistry = registry.getOrDefault(type, null);
        if (typeRegistry == null) return null;
        ArrayList<ParameterList> possibleParameterLists = typeRegistry.getOrDefault(key.getName(), null);
        if (possibleParameterLists == null) return null;
        for (ParameterList pl: possibleParameterLists) {
            List<Object> posParams = null;
            if (pl.getPositionalParams() != null) {
                posParams = matchPositionalParameters(key.getPositionalParams(), pl.getPositionalParams());
            }
            if (posParams == null && pl.getPositionalParams() != null) continue;
            Map<String, Object> kvParams = null;
            if (pl.getKvParams() != null) {
                kvParams = matchKVParams(key.getKvParams(), pl.getKvParams());
            }
            if (kvParams == null && pl.getKvParams() != null) continue;
            return pl.getFactory().apply(posParams, kvParams);
        }
        throw new WrongConfigKeyException();
    }

    private static class MismatchedTypeException extends Exception {}

    private Object createMatchingObject(ConfigExpression s, Parameter t) throws WrongConfigKeyException, WrongExpressionTypeException, MismatchedTypeException {
        if (s.getType() == ConfigExpression.Type.NUMBER) {
            if (Integer.class.equals(t.getType()) || int.class.equals(t.getType())) {
                return s.getInt();
            } else if (Long.class.equals(t.getType()) || long.class.equals(t.getType())) {
                return s.getLong();
            } else if (Double.class.equals(t.getType()) || double.class.equals(t.getType())) {
                return s.getDouble();
            }
        } else if (s.getType() == ConfigExpression.Type.BOOL && (Boolean.class.equals(t.getType()) || boolean.class.equals(t.getType()))) {
            return s.getBool();
        } else if (s.getType() == ConfigExpression.Type.STRING && String.class.equals(t.getType())) {
            return s.getString();
        } else if (s.getType() == ConfigExpression.Type.CONFIG_KEY) {
            return create(t.getType(), s.getConfigKey());
        }
        throw new MismatchedTypeException();
    }

    private List<Object> matchPositionalParameters(List<ConfigExpression> source, List<Parameter> target) {
        if (source.size() > target.size()) return null;
        ArrayList<Object> res = new ArrayList<>();
        int i = 0;
        for (; i < source.size(); ++i) {
            ConfigExpression s = source.get(i);
            Parameter t = target.get(i);
            try {
                res.add(createMatchingObject(s, t));
            } catch (WrongExpressionTypeException | WrongConfigKeyException | MismatchedTypeException e) {
                return null;
            }
        }
        for (; i < target.size(); ++i) {
            Parameter t = target.get(i);
            if (t.isRequired()) return null;
            res.add(t.getDefaultValue());
        }
        return res;
    }

    private Map<String, Object> matchKVParams(Map<String, ConfigExpression> source, Map<String, Parameter> target) {
        if (source.size() > target.size()) return null;
        HashMap<String, Object> res = new HashMap<>();
        for (String key: target.keySet()) {
            Parameter t = target.get(key);
            ConfigExpression s = source.getOrDefault(key, null);
            if (s == null) {
                if (t.isRequired()) return null;
                res.put(key, t.getDefaultValue());
            } else {
                try {
                    res.put(key, createMatchingObject(s, t));
                } catch (WrongExpressionTypeException e) {
                    return null;
                } catch (WrongConfigKeyException e) {
                    return null;
                } catch (MismatchedTypeException e) {
                    return null;
                }
            }
        }
        for (String key: source.keySet()) {
            if (!target.containsKey(key)) return null;
        }
        return res;
    }
}
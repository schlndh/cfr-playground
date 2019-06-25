package com.ggp.parsers;

import com.ggp.parsers.exceptions.ConfigAssemblyException;
import com.ggp.parsers.exceptions.TypeFactoryException;
import com.ggp.parsers.exceptions.WrongConfigKeyException;
import com.ggp.parsers.exceptions.WrongExpressionTypeException;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

public class ConfigurableFactory {
    public static class ConfigurableImplementation {
        private ArrayList<ParameterList> factories = new ArrayList<>();
        private String description = "";

        public List<ParameterList> getFactories() {
            return Collections.unmodifiableList(factories);
        }

        void addFactory(ParameterList parameters) {
            factories.add(parameters);
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }

    public static class ConfigurableType {
        private HashMap<String, ConfigurableImplementation> registry = new HashMap<>();
        private String description = "";

        HashMap<String, ConfigurableImplementation> getModifiableRegistry() {
            return registry;
        }

        public Map<String, ConfigurableImplementation> getRegisteredImplementations() {
            return Collections.unmodifiableMap(registry);
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }

    private HashMap<Class<?>, ConfigurableType> registry = new HashMap<>();

    public void register(Class<?> type, String name, ParameterList parameters) {
        registry.computeIfAbsent(type, a -> new ConfigurableType()).getModifiableRegistry()
                .computeIfAbsent(name, b -> new ConfigurableImplementation())
                .addFactory(parameters);
    }

    public void register(Class<?> type, String name, ParameterList parameters, String description) {
        register(type, name, parameters);
        registry.get(type).getRegisteredImplementations().get(name).setDescription(description);
    }

    public static ParameterList createPositionalParameterList(Constructor<?> constructor, String ... parameterDescriptions) {
        ArrayList<Parameter> params = new ArrayList<>();
        if (parameterDescriptions == null) {
            parameterDescriptions = new String[0];
        }
        int parameterIdx = 0;
        for (Class<?> c: constructor.getParameterTypes()) {
            String desc = parameterDescriptions.length > parameterIdx ? parameterDescriptions[parameterIdx] : "";
            params.add(new Parameter(c, null, true, desc));
            parameterIdx++;
        }
        String plDesc = parameterDescriptions.length > parameterIdx ? parameterDescriptions[parameterIdx] : "";
        return new ParameterList(params, null, (posParams, kvParams) -> {
            try {
                return constructor.newInstance(posParams.toArray());
            } catch (InvocationTargetException e) {
                if (e.getCause() == null) {
                    throw new RuntimeException(e);
                } else {
                    throw new TypeFactoryException(e.getCause());
                }
            } catch (IllegalAccessException | InstantiationException e) {
                throw new RuntimeException(e);
            }
        }, plDesc);
    }

    public <T> T create(Class<T> type, ConfigKey key) throws ConfigAssemblyException {
        return (T) doCreate(type, key);
    }

    public <T> T create(Class<T> type, ConfigExpression expr) throws ConfigAssemblyException {
        return (T) doCreate(type, expr);
    }

    public Map<Class<?>, ConfigurableType> getRegistry() {
        return Collections.unmodifiableMap(registry);
    }

    public void setTypeDescription(Class<?> c, String description) {
        registry.computeIfAbsent(c, a -> new ConfigurableType()).setDescription(description);
    }

    public void setImplementationDescription(Class<?> c, String name, String description) {
        registry.get(c).getRegisteredImplementations().get(name).setDescription(description);
    }

    public Set<Class<?>> getRegisteredTypes() {
        return Collections.unmodifiableSet(registry.keySet());
    }

    private Object doCreate(Class<?> type, ConfigKey key) throws ConfigAssemblyException {
        if (key == null) return null;
        ConfigurableType confType = registry.getOrDefault(type, null);
        if (confType == null) return null;
        HashMap<String, ConfigurableImplementation> typeRegistry = confType.getModifiableRegistry();
        if (!typeRegistry.containsKey(key.getName())) throw new WrongConfigKeyException();
        ConfigurableImplementation impl = typeRegistry.getOrDefault(key.getName(), null);
        if (impl == null) return null;
        for (ParameterList pl: impl.getFactories()) {
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

    private Object doCreate(Class<?> type, ConfigExpression s) throws ConfigAssemblyException {
        if (s.getType() == ConfigExpression.Type.NUMBER) {
            if (Integer.class.equals(type) || int.class.equals(type)) {
                return s.getInt();
            } else if (Long.class.equals(type) || long.class.equals(type)) {
                return s.getLong();
            } else if (Double.class.equals(type) || double.class.equals(type)) {
                return s.getDouble();
            }
        } else if (s.getType() == ConfigExpression.Type.ARRAY) {
            Class<?> arrClass = type;
            if (!arrClass.isArray()) return null;
            List<ConfigExpression> arrValues = s.getArrayValues();
            Object arr = Array.newInstance(arrClass.getComponentType(), arrValues.size());
            int i = 0;
            for (ConfigExpression expr: arrValues) {
                Array.set(arr, i++, doCreate(arrClass.getComponentType(), expr));
            }
            return arr;
        } else if (s.getType() == ConfigExpression.Type.BOOL && (Boolean.class.equals(type) || boolean.class.equals(type))) {
            return s.getBool();
        } else if (s.getType() == ConfigExpression.Type.STRING && String.class.equals(type)) {
            return s.getString();
        } else if (s.getType() == ConfigExpression.Type.CONFIG_KEY) {
            return doCreate(type, s.getConfigKey());
        }
        throw new WrongExpressionTypeException();
    }

    private List<Object> matchPositionalParameters(List<ConfigExpression> source, List<Parameter> target) {
        if (source.size() > target.size()) return null;
        ArrayList<Object> res = new ArrayList<>();
        int i = 0;
        for (; i < source.size(); ++i) {
            ConfigExpression s = source.get(i);
            Parameter t = target.get(i);
            try {
                res.add(doCreate(t.getType(), s));
            } catch (ConfigAssemblyException e) {
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
                    res.put(key, doCreate(t.getType(), s));
                } catch (ConfigAssemblyException e) {
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

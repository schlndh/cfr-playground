package com.ggp.parsers.visitors;

import com.ggp.parsers.ConfigKey;
import com.ggp.parsers.ConfigKeyBaseVisitor;
import com.ggp.parsers.ConfigKeyParser;

public class ConfigKeyVisitor extends ConfigKeyBaseVisitor<ConfigKey> {
    @Override
    public ConfigKey visitConfigKey(ConfigKeyParser.ConfigKeyContext ctx) {
        String name = ctx.objectName().getText();
        ConfigKey ck = new ConfigKey(name);
        ParamVisitor visitor = new ParamVisitor(ck);
        ConfigKeyParser.ParamsContext paramsCtx = ctx.params();
        visitor.visit(paramsCtx);
        return ck;
    }
}

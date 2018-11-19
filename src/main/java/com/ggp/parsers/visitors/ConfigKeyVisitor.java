package com.ggp.parsers.visitors;

import com.ggp.parsers.ConfigKey;
import com.ggp.parsers.ConfigKeyBaseVisitor;
import com.ggp.parsers.ConfigKeyParser;

public class ConfigKeyVisitor extends ConfigKeyBaseVisitor<ConfigKey> {
    @Override
    public ConfigKey visitConfigKey(ConfigKeyParser.ConfigKeyContext ctx) {
        String name = ctx.objectName().getText();
        ParamVisitor visitor = new ParamVisitor();
        ConfigKeyParser.ParamsContext paramsCtx = ctx.params();
        visitor.visit(paramsCtx);
        ConfigKey ck = new ConfigKey(name, visitor.getPosParams(), visitor.getKvParams());
        return ck;
    }
}

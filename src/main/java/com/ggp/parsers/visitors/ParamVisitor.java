package com.ggp.parsers.visitors;

import com.ggp.parsers.*;

public class ParamVisitor extends ConfigKeyBaseVisitor<ConfigKey> {
    private ConfigKey configKey;

    public ParamVisitor(ConfigKey configKey) {
        this.configKey = configKey;
    }


    @Override
    public ConfigKey visitPosParam(ConfigKeyParser.PosParamContext ctx) {
        ConfigExpressionVisitor visitor = new ConfigExpressionVisitor();
        ConfigExpression expr = visitor.visit(ctx.expr());
        if (expr != null) configKey.addPositionalParam(expr);
        return configKey;
    }

    @Override
    public ConfigKey visitKvParam(ConfigKeyParser.KvParamContext ctx) {
        ConfigExpressionVisitor visitor = new ConfigExpressionVisitor();
        ConfigExpression expr = visitor.visit(ctx.kvVal().expr());
        String key = ctx.kvKey().getText();
        if (expr != null) configKey.addKVParam(key, expr);
        return configKey;
    }
}

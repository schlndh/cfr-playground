package com.ggp.parsers.visitors;

import com.ggp.parsers.*;

import java.util.ArrayList;
import java.util.HashMap;

public class ParamVisitor extends ConfigKeyBaseVisitor<Void> {
    private ArrayList<ConfigExpression> posParams = new ArrayList<>();
    private HashMap<String, ConfigExpression> kvParams = new HashMap<>();

    @Override
    public Void visitPosParam(ConfigKeyParser.PosParamContext ctx) {
        ConfigExpressionVisitor visitor = new ConfigExpressionVisitor();
        ConfigExpression expr = visitor.visit(ctx.expr());
        if (expr != null) posParams.add(expr);
        return null;
    }

    @Override
    public Void visitKvParam(ConfigKeyParser.KvParamContext ctx) {
        ConfigExpressionVisitor visitor = new ConfigExpressionVisitor();
        ConfigExpression expr = visitor.visit(ctx.kvVal().expr());
        String key = ctx.kvKey().getText();
        if (expr != null) kvParams.put(key, expr);
        return null;
    }

    public ArrayList<ConfigExpression> getPosParams() {
        return posParams;
    }

    public HashMap<String, ConfigExpression> getKvParams() {
        return kvParams;
    }
}

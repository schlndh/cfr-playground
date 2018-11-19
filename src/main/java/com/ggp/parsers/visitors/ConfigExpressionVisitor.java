package com.ggp.parsers.visitors;

import com.ggp.parsers.*;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.TerminalNode;

public class ConfigExpressionVisitor extends ConfigKeyBaseVisitor<ConfigExpression> {
    @Override
    public ConfigExpression visitExpr(ConfigKeyParser.ExprContext ctx) {
        ConfigExpression expr = null;
        if (ctx.getChild(0) instanceof TerminalNode) {
            Token token = ((TerminalNode) ctx.getChild(0)).getSymbol();
            if (token.getType() == ConfigKeyLexer.NULL) {
                expr = ConfigExpression.createConfigKey(null);
            } else {
                String strValue = token.getText();
                if (token.getType() == ConfigKeyLexer.STRING) {
                    strValue = strValue.substring(1, strValue.length() - 1); // remove double quotes
                    strValue = strValue.replace("\\t", "\t");
                    strValue = strValue.replace("\\b", "\b");
                    strValue = strValue.replace("\\n", "\n");
                    strValue = strValue.replace("\\r", "\r");
                    strValue = strValue.replace("\\\"", "\"");
                    strValue = strValue.replace("\\\\", "\\");
                }
                expr = ConfigExpression.createValueType(ConfigExpression.intToType(token.getType()), strValue);
            }
        } else if (ctx.configKey() != null) {
            expr = ConfigExpression.createConfigKey(new ConfigKeyVisitor().visit(ctx.configKey()));
        } else if (ctx.array() != null) {
            ParamVisitor visitor = new ParamVisitor();
            visitor.visit(ctx.array());
            expr = ConfigExpression.createArray(visitor.getPosParams());
        }
        return expr;
    }
}

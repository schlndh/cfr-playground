package com.ggp.parsers;

import com.ggp.parsers.visitors.ConfigExpressionVisitor;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.ParseCancellationException;

public class ParseUtils {
    public static ConfigExpression parseConfigExpression(String str) {
        CharStream inputStream = CharStreams.fromString(str);
        ConfigKeyLexer lexer = new ConfigKeyLexer(inputStream);
        CommonTokenStream tokenStream = new CommonTokenStream(lexer);
        ConfigKeyParser parser = new ConfigKeyParser(tokenStream);
        parser.setErrorHandler(new BailErrorStrategy());

        ConfigExpressionVisitor visitor = new ConfigExpressionVisitor();
        try {
            return visitor.visit(parser.expr());
        } catch (ParseCancellationException e) {

        }
        return null;
    }
}

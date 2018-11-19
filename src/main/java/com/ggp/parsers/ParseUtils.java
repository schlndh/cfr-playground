package com.ggp.parsers;

import com.ggp.parsers.visitors.ConfigKeyVisitor;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.ParseCancellationException;

public class ParseUtils {
    public static ConfigKey parseConfigKey(String str) {
        CharStream inputStream = CharStreams.fromString(str);
        ConfigKeyLexer lexer = new ConfigKeyLexer(inputStream);
        CommonTokenStream tokenStream = new CommonTokenStream(lexer);
        ConfigKeyParser parser = new ConfigKeyParser(tokenStream);
        parser.setErrorHandler(new BailErrorStrategy());

        ConfigKeyVisitor visitor = new ConfigKeyVisitor();
        try {
            return visitor.visit(parser.configKey());
        } catch (ParseCancellationException e) {

        }
        return null;
    }
}

package com.ggp.parsers;

import com.ggp.IGameDescription;
import com.ggp.parsers.visitors.ConfigKeyVisitor;
import com.ggp.utils.GameRepository;
import com.ggp.utils.recall.PerfectRecallGameDescriptionWrapper;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.antlr.v4.runtime.tree.TerminalNode;

public class ParseUtils {
    public static IGameDescription parseGameDescription(String str) {
        CharStream inputStream = CharStreams.fromString(str);
        GamesLexer lexer = new GamesLexer(inputStream);
        CommonTokenStream tokenStream = new CommonTokenStream(lexer);
        GamesParser gp = new GamesParser(tokenStream);
        gp.setErrorHandler(new BailErrorStrategy());

        GamesVisitor<IGameDescription> gamesVisitor = new GamesBaseVisitor<IGameDescription>() {
            @Override
            public IGameDescription visitLeducPoker(GamesParser.LeducPokerContext ctx) {
                return GameRepository.leducPoker(parseInt(ctx.POSINT(0)), parseInt(ctx.POSINT(1)));
            }

            @Override
            public IGameDescription visitIiGoofspiel(GamesParser.IiGoofspielContext ctx) {
                return GameRepository.iiGoofspiel(parseInt(ctx.POSINT()));
            }

            @Override
            public IGameDescription visitRps(GamesParser.RpsContext ctx) {
                return GameRepository.rps(parseInt(ctx.POSINT()));
            }

            @Override
            public IGameDescription visitKriegTTT(GamesParser.KriegTTTContext ctx) {
                return GameRepository.kriegTTT();
            }

            @Override
            public IGameDescription visitPerfRecall(GamesParser.PerfRecallContext ctx) {
                return new PerfectRecallGameDescriptionWrapper(ctx.game().accept(this));
            }

            private int parseInt(TerminalNode node) {
                return Integer.valueOf(node.getSymbol().getText());
            }
        };

        try {
            GamesParser.GameContext gameCtx = gp.game();
            return gamesVisitor.visit(gameCtx);
        } catch (ParseCancellationException e) {
        }
        return null;
    }

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

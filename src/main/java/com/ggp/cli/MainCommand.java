package com.ggp.cli;

import com.ggp.parsers.ConfigurableFactory;
import picocli.CommandLine;

@CommandLine.Command(name="./CFRPlayground", subcommands = {
        RunCommand.class,
        TournamentCommand.class,
        CommandLine.HelpCommand.class,
        SolveCommand.class,
        EvaluateCommand.class,
        GameInfoCommand.class,
        MergeGamePlayingResultsCommand.class,
        CFRDEvalCommand.class,
        ConfigHelpCommand.class,
        GPToCSVCommand.class}, mixinStandardHelpOptions=true)
public class MainCommand implements Runnable {
    ConfigurableFactory configurableFactory = new ConfigurableFactory();

    public ConfigurableFactory getConfigurableFactory() {
        return configurableFactory;
    }

    @Override
    public void run() {
        System.out.println("No sub-command was selected! Run './CFRPlayground help' to see the list of available sub-commands.");
    }
}

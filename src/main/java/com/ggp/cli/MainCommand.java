package com.ggp.cli;

import com.ggp.parsers.ConfigurableFactory;
import picocli.CommandLine;

@CommandLine.Command(name="./GeneralDeepstack", subcommands = {
        RunCommand.class,
        HelpCommand.class,
        SolveCommand.class,
        EvaluateCommand.class,
        GameInfoCommand.class,
        MergeGamePlayingResultsCommand.class,
        CFRDEvalCommand.class,
        GPToCSVCommand.class}, mixinStandardHelpOptions=true)
public class MainCommand implements Runnable {
    ConfigurableFactory configurableFactory = new ConfigurableFactory();

    public ConfigurableFactory getConfigurableFactory() {
        return configurableFactory;
    }

    @Override
    public void run() {
    }
}

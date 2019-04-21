package com.ggp.cli;

import com.ggp.parsers.ConfigurableFactory;
import com.ggp.parsers.Parameter;
import com.ggp.parsers.ParameterList;
import picocli.CommandLine;

import java.util.Map;

@CommandLine.Command(name = "config-help",
        mixinStandardHelpOptions = true,
        description = "Provides information about available configurable types",
        optionListHeading = "%nOptions:%n",
        sortOptions = false
)
public class ConfigHelpCommand implements Runnable {
    @CommandLine.ParentCommand
    private MainCommand mainCommand;

    @CommandLine.Spec
    CommandLine.Model.CommandSpec commandSpec;

    @CommandLine.Option(names = {"-t", "--type"}, description = "Type name (substring of the canonical type name)")
    private String type;

    @CommandLine.Option(names = {"-i", "--impl"}, description = "Implementation name")
    private String impl;

    private int getTableWidth() {
        return commandSpec.usageMessage().width();
    }

    private CommandLine.Help.TextTable createTable(int firstColWidth ) {
        int width = getTableWidth();
        CommandLine.Help.Ansi ansi = CommandLine.Help.Ansi.AUTO;
        CommandLine.Help.TextTable textTable = CommandLine.Help.TextTable.forColumns(ansi,
                new CommandLine.Help.Column(firstColWidth + 2, 2, CommandLine.Help.Column.Overflow.SPAN),
                new CommandLine.Help.Column(width - (firstColWidth + 2), 2, CommandLine.Help.Column.Overflow.WRAP));
        return textTable;
    }

    private String classToString(Class<?> c) {
        return c.getCanonicalName();
    }

    private void printFactories(Class<?> confTypeClass, ConfigurableFactory.ConfigurableType confType, ConfigurableFactory.ConfigurableImplementation confImpl) {
        System.out.println("Implementation " + impl + " of type " + classToString(confTypeClass));
        String implDesc = confImpl.getDescription();
        if (implDesc != null && implDesc.length() > 0) {
            System.out.println(implDesc);
        }
        for (ParameterList pl: confImpl.getFactories()) {
            System.out.println();
            System.out.println(pl.getDescription());
            if (pl.getPositionalParams() != null && pl.getPositionalParams().size() > 0) {
                System.out.println("Positional parameters:");
                CommandLine.Help.TextTable textTable = createTable(40);
                for (Parameter p: pl.getPositionalParams()) {
                    textTable.addRowValues(classToString(p.getType()), p.getDescription());
                }
                System.out.println(textTable.toString());
            }
            if (pl.getKvParams() != null &&  pl.getKvParams().size() > 0) {
                System.out.println("Key-value parameters:");
                for (Map.Entry<String, Parameter> entry: pl.getKvParams().entrySet()) {
                    System.out.println(entry.getKey() + "\t" + classToString(entry.getValue().getType()));
                    String desc = entry.getValue().getDescription();
                    if (desc != null && desc.length() > 0) {
                        System.out.println("\t" + desc);
                        System.out.println();
                    }
                }
            }
        }
    }

    @Override
    public void run() {
        if (type == null && impl == null) {
            CommandLine.Help.TextTable textTable = createTable(40);
            textTable.addRowValues("Type", "Description");
            textTable.addEmptyRow();
            for (Map.Entry<Class<?>, ConfigurableFactory.ConfigurableType> entry :
                    mainCommand.getConfigurableFactory().getRegistry().entrySet()) {
                textTable.addRowValues(classToString(entry.getKey()), entry.getValue().getDescription());
            }
            System.out.println(textTable.toString());
        } else if (type != null) {
            for (Map.Entry<Class<?>, ConfigurableFactory.ConfigurableType> entry :
                    mainCommand.getConfigurableFactory().getRegistry().entrySet()) {
                if (entry.getKey().getCanonicalName().contains(type)) {
                    if (impl == null) {
                        System.out.println("Registered implementations of type " + classToString(entry.getKey()));
                        CommandLine.Help.TextTable textTable = createTable(40);
                        textTable.addRowValues("Implementation", "Description");
                        textTable.addEmptyRow();
                        for (Map.Entry<String, ConfigurableFactory.ConfigurableImplementation> implEntry:
                                entry.getValue().getRegisteredImplementations().entrySet()) {
                            textTable.addRowValues(implEntry.getKey(), implEntry.getValue().getDescription());
                        }
                        System.out.println(textTable.toString());
                    } else {
                        ConfigurableFactory.ConfigurableImplementation confImpl = entry.getValue().getRegisteredImplementations().getOrDefault(impl, null);
                        if (confImpl == null) continue;
                        printFactories(entry.getKey(), entry.getValue(), confImpl);
                    }
                }
            }
        } else if (impl != null) {
            for (Map.Entry<Class<?>, ConfigurableFactory.ConfigurableType> entry :
                    mainCommand.getConfigurableFactory().getRegistry().entrySet()) {
                ConfigurableFactory.ConfigurableImplementation confImpl = entry.getValue().getRegisteredImplementations().getOrDefault(impl, null);
                if (confImpl == null) continue;
                printFactories(entry.getKey(), entry.getValue(), confImpl);
            }
        }
    }
}
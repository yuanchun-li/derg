package com.yli.derg;

/**
 * Created by LiYC on 2015/7/18.
 * Package: DERG
 */
import com.yli.derg.backends.DERGBackend;
import com.yli.derg.frontends.DERGFrontend;
import com.yli.derg.utils.*;
import org.apache.commons.cli.*;

import java.io.*;
import java.util.Comparator;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.SimpleFormatter;

public class Config {
    public static final String PROJECT_NAME = "DERG";

    // Directory for input
    public static String inputDirOrFile = "";
    public static String frontendType = "";
    public static DERGFrontend dergFrontend;

    // Directory for result output
    public static String outputDir = "output";
    public static String backendType = "";
    public static DERGBackend dergBackend;

    private static PrintStream resultPs;

    public static void init() {
        DERGFrontend.registerFrontends();
        DERGBackend.registerBackends();
    }

    public static boolean parseArgs(String[] args) {
        Options options = new Options();
        Option quiet = new Option("quiet", "be extra quiet");
        Option debug = new Option("debug", "print debug information");
        Option output = Option.builder("o").argName("directory").required()
                .longOpt("output").hasArg().desc("path to output dir").build();
        Option input = Option.builder("i").argName("directory/file").required()
                .longOpt("input").hasArg().desc("path to target program").build();
        Option frontend = Option.builder("f").argName("frontend").longOpt("frontend").hasArg()
                .desc(String.format("DERG frontend: %s", DERGFrontend.getAvailableFrontends())).build();
        Option backend = Option.builder("b").argName("backend").longOpt("backend").hasArg()
                .desc(String.format("DERG backend: %s", DERGBackend.getAvailableBackends())).build();

        options.addOption(quiet);
        options.addOption(debug);
        options.addOption(output);
        options.addOption(input);
        options.addOption(frontend);
        options.addOption(backend);

        CommandLineParser parser = new IgnoreUnknownTokenParser();

        try {
            CommandLine cmd = parser.parse(options, args);
            if (cmd.hasOption("debug")) Util.LOGGER.setLevel(Level.ALL);
            if (cmd.hasOption("quiet")) Util.LOGGER.setLevel(Level.WARNING);

            if (cmd.hasOption("i")) {
                Config.inputDirOrFile = cmd.getOptionValue("i");
                File codeDirFile = new File(Config.inputDirOrFile);
                if (!codeDirFile.exists()) {
                    throw new ParseException("Input dir/file does not exist.");
                }
            }
            if (cmd.hasOption('o')) {
                Config.outputDir = cmd.getOptionValue('o');
                File workingDir = new File(Config.outputDir);
                Config.outputDir = workingDir.getPath();
                if (!workingDir.exists() && !workingDir.mkdirs()) {
                    throw new ParseException("Error generating output directory.");
                }
            }
            if (cmd.hasOption('f')) {
                Config.frontendType = cmd.getOptionValue('f');
            }
            Config.dergFrontend = com.yli.derg.frontends.DERGFrontend.getBuilder(Config.frontendType);
            if (Config.dergFrontend == null) {
                throw new ParseException("unknown frontend type: " + Config.frontendType);
            }
            Config.dergFrontend.parseArgs(args);
            if (cmd.hasOption('b')) {
                Config.backendType = cmd.getOptionValue('b');
            }
            Config.dergBackend = com.yli.derg.backends.DERGBackend.get(Config.backendType);
            if (Config.dergBackend == null) {
                throw new ParseException("unknown backend type: " + Config.backendType);
            }
            Config.dergBackend.parseArgs(args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            HelpFormatter formatter = new HelpFormatter();
            formatter.setOptionComparator(new Comparator<Option>() {
                @Override
                public int compare(Option o1, Option o2) {
                    return o1.getOpt().length() - o2.getOpt().length();
                }
            });
            formatter.printHelp(Config.PROJECT_NAME, options, true);
            return false;
        }

        File logFile = new File(String.format("%s/derg.log", Config.outputDir));

        try {
            FileHandler fh = new FileHandler(logFile.getAbsolutePath());
            fh.setFormatter(new SimpleFormatter());
            Util.LOGGER.addHandler(fh);
        } catch (IOException e) {
            e.printStackTrace();
        }

        Util.LOGGER.info("finish parsing arguments");
        Util.LOGGER.info(String.format("[frontend]%s, [input]%s, [output]%s",
                Config.frontendType, Config.inputDirOrFile, Config.outputDir));
        return true;
    }
}
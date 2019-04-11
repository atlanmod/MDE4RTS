package com.tblf;

import com.tblf.business.AnalysisLauncher;
import com.tblf.compare.GitCaller;
import com.tblf.instrumentation.InstrumentationType;
import com.tblf.junitrunner.MavenRunner;
import com.tblf.parsing.TraceType;
import com.tblf.parsing.parsingBehaviors.EmptyParsingBehavior;
import org.apache.commons.cli.*;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Main class of the application. Analyses the command line and run the RTS & Impact Analysis
 */
public class App {
    private static final Logger LOGGER = Logger.getLogger(App.class.getName());

    /**
     * Main class. Analysis parameters and acts accordingly
     *
     * @param args an Array of {@link String}
     * @throws ParseException if the command line cannot be parsed
     */
    public static void main(String[] args) throws ParseException {

        CommandLineParser commandLineParser = new DefaultParser();
        Options options = new Options();

        options.addOption("help", false, "print this message");
        options.addOption("iamodel", false, "build the impact analysis model without performing a RTS");
        options.addOption("model", false, "build the static source code model");
        options.addOption(Option.builder("rts").optionalArg(true).argName("commitID").hasArg(true).desc("perform a regression test selection between the current revision and the specified commit. Specify no commit if you want to apply RTS between master and HEAD").build());
        options.addOption(Option.builder("project").hasArg(true).optionalArg(false).argName("projectURI").desc("project on which the operations will be performed").build());
        CommandLine commandLine = commandLineParser.parse(options, args);

        if (args.length == 0)
            new HelpFormatter().printHelp("java -jar mde4rts.jar ", options);

        if (commandLine.hasOption("help"))
            new HelpFormatter().printHelp("java -jar mde4rts.jar ", options);

        File file = null;
        if (!commandLine.hasOption("project") && !commandLine.hasOption("help") && ! (args.length == 0)) {
            System.out.println("No project specified ! -help for more information");
        } else if (commandLine.hasOption("project")) {
            file = new File(commandLine.getOptionValue("project"));
        }

        if (commandLine.hasOption("model")) {
            new App().buildStaticModel(file);
        } else if (commandLine.hasOption("iamodel")) {    //Impact Analysis Model
            App app = new App();
            app.buildStaticModel(file);
            app.buildImpactAnalysisModel(file);
        }

        if (commandLine.hasOption("rts")) {
            String commitID = commandLine.getOptionValue("rts");

            Collection<String> testImpacted;
            if (commitID == null) {
                //RTS master, HEAD
                System.out.println("Performing a Regression Test Selection since last commit, using the last impact analysis model built and HEAD");
                testImpacted = new GitCaller(file).diffsSinceLastCommit();
            } else {
                //RTS commit, HEAD
                System.out.println("Performing a Regression Test Selection between " + commitID + " and HEAD");
                testImpacted = new GitCaller(file).traceBasedCommitComparison(commitID, "HEAD");
            }

            LOGGER.info("Test(s) impacted: ");
            testImpacted.forEach(LOGGER::info);
        }

    }

    /**
     * Build the impact analysis model of a project passed as a parameter
     *
     * @param file the {@link File} location of the project under analysis
     */
    public void buildImpactAnalysisModel(File file) {
        new MavenRunner(new File(file, "pom.xml")).compilePom();
        File agent = null;
        try {
            agent = new File(getClass().getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
            if (agent.isDirectory()) {
                agent = new File("target/mde4rts-jar-with-dependencies.jar");
            }
        } catch (URISyntaxException e) {
            LOGGER.log(Level.WARNING, "Could not get the Agent location", e);
        }

        if (!agent.exists())
            throw new RuntimeException("No java agent found");

        AnalysisLauncher analysisLauncher = new AnalysisLauncher(file);
        analysisLauncher.setInstrumentationType(InstrumentationType.AGENT);
        analysisLauncher.setTraceType(TraceType.QUEUE);
        analysisLauncher.registerProcessor(agent);
        analysisLauncher.registerBehavior(new EmptyParsingBehavior());
        analysisLauncher.run();
    }

    public void buildStaticModel(File file) {
        System.out.println("Discovering " + file.getAbsolutePath());
        new Discoverer(file).run();
    }
}

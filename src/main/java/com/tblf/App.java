package com.tblf;

import com.tblf.business.AnalysisLauncher;
import com.tblf.compare.GitCaller;
import com.tblf.instrumentation.InstrumentationType;
import com.tblf.junitrunner.MavenRunner;
import com.tblf.parsing.TraceType;
import com.tblf.parsingbehaviors.MethodGrainedImpactAnalysisBehavior;
import com.tblf.processors.CallGraphProcessor;
import com.tblf.utils.ModelUtils;
import org.apache.commons.cli.*;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.*;
import org.eclipse.jgit.merge.MergeStrategy;
import org.eclipse.jgit.revwalk.RevCommit;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
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
        options.addOption("model", false, "build the impact analysis model without performing a RTS");
        options.addOption(Option.builder("rts").optionalArg(true).argName("commitID").hasArg(true).desc("perform a regression test selection between the current revision and the specified commit. Specify no commit if you want to apply RTS between master and HEAD").build());
        options.addOption(Option.builder("project").hasArg(true).optionalArg(false).argName("projectURI").desc("project on which the operations will be performed").build());
        CommandLine commandLine = commandLineParser.parse(options, args);

        if (args.length == 0)
            new HelpFormatter().printHelp("./mde4rts.jar", options);

        if (commandLine.hasOption("help"))
            new HelpFormatter().printHelp("./mde4rts.jar", options);

        File file = null;
        if (!commandLine.hasOption("project") && !commandLine.hasOption("help")) {
            System.out.println("No project specified ! -help for more information");
        } else if (commandLine.hasOption("project")) {
            file = new File(commandLine.getOptionValue("project"));
        }

        if (commandLine.hasOption("model"))    //Impact Analysis Model
            new App().buildImpactAnalysisModel(file);


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
                testImpacted = new GitCaller(file).compareCommits(commitID, "HEAD");
            }

            System.out.println("Test(s) impacted: ");
            testImpacted.forEach(System.out::println);
        }

    }

    /**
     * Build the impact analysis model of a project passed as a parameter
     *
     * @param file the {@link File} location of the project under analysis
     */
    public void buildImpactAnalysisModel(File file) {
        System.out.println("Discovering " + file.getAbsolutePath());
        new Discoverer(file).run();

        new MavenRunner(new File(file, "pom.xml")).compilePom();

        ResourceSet resourceSet = ModelUtils.buildResourceSet(file);

        AnalysisLauncher analysisLauncher = new AnalysisLauncher(file);
        analysisLauncher.setInstrumentationType(InstrumentationType.BYTECODE);
        analysisLauncher.setTraceType(TraceType.FILE);
        analysisLauncher.registerDependencies(Collections.singletonList(new File("pom.xml")));
        analysisLauncher.registerProcessor(new CallGraphProcessor());
        analysisLauncher.registerBehavior(new MethodGrainedImpactAnalysisBehavior(resourceSet));
        analysisLauncher.applyAfter(file1 -> {
            try {
                ((MethodGrainedImpactAnalysisBehavior) analysisLauncher.getBehavior()).close();
                long timeBefore = System.currentTimeMillis();
                resourceSet.getResources().get(resourceSet.getResources().size() - 1).save(Collections.EMPTY_MAP);
                System.out.println("saving the model lasted: " + (System.currentTimeMillis() - timeBefore) + " ms");
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Could not save the impact analysis model", e);
            }
        });
        analysisLauncher.run();
    }
}

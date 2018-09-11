package com.tblf;

import com.tblf.business.AnalysisLauncher;
import com.tblf.instrumentation.InstrumentationType;
import com.tblf.junitrunner.MavenRunner;
import com.tblf.parsing.TraceType;
import com.tblf.parsing.indexer.HawkQuery;
import com.tblf.parsing.parsers.Parser;
import com.tblf.parsing.traceReaders.TraceQueueReader;
import com.tblf.utils.Configuration;
import com.tblf.utils.ModelUtils;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;

import java.io.File;
import java.io.IOException;
import java.util.Collections;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args )
    {
        if (args.length == 0)
            System.exit(1);

        File file = new File(args[0]);

        buildImpactAnalysisModel(file);

        //new GitCaller(file.getParentFile()).compareCommits("3506ccd", "master");
        //HawkQuery hawkQuery = new HawkQuery(new File("smmGraph.xmi").getParentFile());

    }

    public static void buildImpactAnalysisModel(File file) {

        new MavenRunner(new File(file, "pom.xml")).compilePom();

        AnalysisLauncher analysisLauncher = new AnalysisLauncher(file);
        analysisLauncher.setInstrumentationType(InstrumentationType.BYTECODE);
        analysisLauncher.setTraceType(TraceType.QUEUE);
        analysisLauncher.registerDependencies(Collections.singletonList(new File("pom.xml")));
        analysisLauncher.registerProcessor(new CallGraphProcessor());
        analysisLauncher.applyAfter(file1 -> {
            File trace = new File(file1, Configuration.getProperty("traceFile"));
            ResourceSet resourceSet = ModelUtils.buildResourceSet(file1);
            new Parser(new TraceQueueReader(trace), new CoarseGrainedImpactAnalysisParsingBehavior(resourceSet)).parse();
            Resource resource = resourceSet.getResources().get(resourceSet.getResources().size() - 1);

            try {
                resource.save(Collections.EMPTY_MAP);
            } catch (IOException e) {
                e.printStackTrace();
            }

        });

        analysisLauncher.run();
    }
}

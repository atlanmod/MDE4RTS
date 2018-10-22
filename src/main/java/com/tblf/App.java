package com.tblf;

import com.tblf.business.AnalysisLauncher;
import com.tblf.instrumentation.InstrumentationType;
import com.tblf.junitrunner.MavenRunner;
import com.tblf.parsing.TraceType;
import com.tblf.utils.ModelUtils;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.*;
import org.eclipse.jgit.merge.MergeStrategy;
import org.eclipse.jgit.revwalk.RevCommit;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Hello world!
 */
public class App {
    private static final Logger LOGGER = Logger.getLogger("App");

    public static void main(String[] args) {
        if (args.length == 0)
            System.exit(1);

        File file = new File(args[0]);

        long timeBefore = System.currentTimeMillis();
        if (args.length == 2) {
            buildImpactAnalysisModelWithSpecificCommitID(file, args[1]);
        } else {
            buildImpactAnalysisModel(file);
        }
        System.out.print(System.currentTimeMillis() - timeBefore);
        System.out.println("ms to build the model");



        Collection<String> stringCollection = new GitCaller(file).compareCommits("cf4d367~5", "cf4d367");
        stringCollection.forEach(System.out::println);


    }

    /**
     * Build an impact analysis model using a specific commit ID
     * @param commitId a CommitID as a {@link String}
     * @param file     a {@link File}
     */
    public static void buildImpactAnalysisModelWithSpecificCommitID(File file, String commitId) {
        String tmpBranchName = String.valueOf(System.currentTimeMillis());
        RevCommit revCommit = null;
        Git git = null;
        try {
            git = Git.open(file);
            String branch = git.getRepository().getBranch();
            git.reset().setRef(commitId).call();
            git.checkout().setCreateBranch(true).setName(tmpBranchName).call();
            revCommit = git.commit().setAll(true).setMessage("temporary commit").call();
            git.checkout().setName(branch).call();
        } catch (IOException | GitAPIException e) {
            LOGGER.log(Level.WARNING, "Caught an exception", e);
            e.printStackTrace();
        }

        buildImpactAnalysisModel(file);

        if (git != null) {
            try {
                git.commit().setAll(true).setMessage("added a model on an ancient commit").call();
                git.merge().setStrategy(MergeStrategy.THEIRS).include(revCommit).call();
                git.branchDelete().setBranchNames(tmpBranchName).setForce(true).call();
            } catch (GitAPIException e) {
                e.printStackTrace();
            }
        }

    }

    public static void buildImpactAnalysisModel(File file) {
        System.out.println("Discovering "+file.getAbsolutePath());
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
                System.out.println("save lasted: "+(System.currentTimeMillis()-timeBefore)+" ms");
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        analysisLauncher.run();
    }
}

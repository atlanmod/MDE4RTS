package com.tblf.compare;


import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.diff.DiffAlgorithm;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.dircache.DirCacheIterator;
import org.eclipse.jgit.lib.AnyObjectId;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.FileTreeIterator;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class call basic git methods using the JGit library
 */
public class GitCaller {

    protected static final Logger LOGGER = Logger.getLogger("GitCaller");

    protected Repository repository;

    protected AnyObjectId oldTree;
    protected AnyObjectId newTree;

    protected File pomFolder;
    protected File gitFolder;

    /**
     * Constructor initializing the {@link Git}
     *
     * @param folder a {@link File} directory containing the .git repository
     * @param pomFolder a {@link File} directory containing the pom.xml mvn file
     */
    public GitCaller(File folder, File pomFolder) {
        this.pomFolder = pomFolder;
        this.gitFolder = folder;
        try (Git git = Git.open(folder)) {
            this.repository = git.getRepository();
        } catch (IOException e) {
            throw new RuntimeException("Cannot load the git repository", e);
        }
    }

    /**
     * Constructor initializing the {@link Git}
     * @param folder a {@link File} directory containing a pom.xml mvn file
     */
    public GitCaller(File folder) {
        this.pomFolder = folder;
        this.gitFolder = folder;
        try (Git git = Git.open(folder)) {
            this.repository = git.getRepository();
        } catch (IOException e) {
            throw new RuntimeException("Cannot load the git repository", e);
        }
    }

    /**
     * Compare two given commit ID
     *  @param currentCommitID the first commit ID
     * @param nextCommitID    the next commit ID
     */
    public Collection<String> modelBasedCommitComparison(String currentCommitID, String nextCommitID) {

        LOGGER.info("Comparing commits "+currentCommitID+" and "+nextCommitID);
        try {
            ObjectId current = repository.resolve(currentCommitID);
            ObjectId future = repository.resolve(nextCommitID);

            if (current == null || future == null) {
                throw new IOException("Cannot resolve the commits: " + current + " -> " + future);
            }

            oldTree = new RevWalk(repository).parseCommit(current).getTree();
            newTree = new RevWalk(repository).parseCommit(future).getTree();

            DiffFormatter diffFormatter = createDiffFormater();
            List<DiffEntry> diffEntries = diffFormatter.scan(oldTree, newTree);
            return new ModelBasedRegressionTestSelection(gitFolder, pomFolder, diffFormatter, diffEntries, current).getAllMethodsImpacted();

        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Couldn't build the revision tree", e);
        }
        return new ArrayList<>();
    }

    /**
     * Compare two given commit ID
     *  @param currentCommitID the first commit ID
     * @param nextCommitID    the next commit ID
     */
    public Collection<String> traceBasedCommitComparison(String currentCommitID, String nextCommitID) {

        LOGGER.info("Comparing commits "+currentCommitID+" and "+nextCommitID);
        try {
            ObjectId current = repository.resolve(currentCommitID);
            ObjectId future = repository.resolve(nextCommitID);

            if (current == null || future == null) {
                throw new IOException("Cannot resolve the commits: " + current + " -> " + future);
            }

            oldTree = new RevWalk(repository).parseCommit(current).getTree();
            newTree = new RevWalk(repository).parseCommit(future).getTree();

            DiffFormatter diffFormatter = createDiffFormater();
            List<DiffEntry> diffEntries = diffFormatter.scan(oldTree, newTree);

            return new TraceBasedRegressionTestSelection(gitFolder, pomFolder, diffFormatter, diffEntries, current).getAllMethodsImpacted();
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Couldn't build the revision tree", e);
        }

        return new ArrayList<>();
    }


    public Collection<String> diffsSinceLastCommit() {
        Collection<String> impactedMethods = null;
        try {
            AbstractTreeIterator newTree = new FileTreeIterator(repository); //Current repo
            AbstractTreeIterator oldTree = new DirCacheIterator(repository.readDirCache()); //old repo
            DiffFormatter diffFormatter = createDiffFormater();
            List<DiffEntry> diffEntries = diffFormatter.scan(oldTree, newTree);
            impactedMethods = new TraceBasedRegressionTestSelection(gitFolder, pomFolder, diffFormatter, diffEntries, repository.resolve("master")).getAllMethodsImpacted();

        } catch (IOException e) {
            e.printStackTrace();
        }

        return impactedMethods;
    }

    private DiffFormatter createDiffFormater() {
        DiffFormatter diffFormatter = new DiffFormatter(System.out);
        diffFormatter.setDiffComparator(RawTextComparator.DEFAULT);
        diffFormatter.setRepository(repository);
        diffFormatter.setDetectRenames(true);
        diffFormatter.setContext(1);
        diffFormatter.setDiffAlgorithm(DiffAlgorithm.getAlgorithm(DiffAlgorithm.SupportedAlgorithm.HISTOGRAM));

        return diffFormatter;
    }
}

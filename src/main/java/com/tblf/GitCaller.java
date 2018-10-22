package com.tblf;


import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.diff.DiffAlgorithm;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class call basic git methods using the JGit library
 */
public class GitCaller {

    protected static final Logger LOGGER = Logger.getLogger("GitCaller");

    protected Repository repository;

    protected RevTree oldTree;
    protected RevTree newTree;

    protected DiffFormatter diffFormatter;
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
    public Collection<String> compareCommits(String currentCommitID, String nextCommitID) {

        LOGGER.info("Comparing commits "+currentCommitID+" and "+nextCommitID);

        try {
            ObjectId current = repository.resolve(currentCommitID);

            ObjectId future = repository.resolve(nextCommitID);

            if (current == null || future == null) {
                throw new IOException("Cannot resolve the commits: " + current + " -> " + future);
            }

            oldTree = new RevWalk(repository).parseCommit(current).getTree();
            newTree = new RevWalk(repository).parseCommit(future).getTree();
            //diffFormatter = new DiffFormatter(new LogOutputStream(LOGGER, Level.FINE));
            diffFormatter = new DiffFormatter(System.out);
            diffFormatter.setDiffComparator(RawTextComparator.DEFAULT);
            diffFormatter.setRepository(repository);
            diffFormatter.setDetectRenames(true);
            diffFormatter.setContext(1);
            diffFormatter.setDiffAlgorithm(DiffAlgorithm.getAlgorithm(DiffAlgorithm.SupportedAlgorithm.HISTOGRAM));
            List<DiffEntry> diffEntryList = diffFormatter.scan(current, future);

            return new RegressionTestSelection(gitFolder, pomFolder, diffFormatter, current, future).getAllMethodsImpacted();
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Couldn't build the revision tree", e);
        }
        return Collections.EMPTY_LIST;
    }
}

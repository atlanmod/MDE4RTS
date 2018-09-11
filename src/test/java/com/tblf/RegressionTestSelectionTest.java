package com.tblf;

import com.tblf.utils.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

public class RegressionTestSelectionTest {

    @Before
    public void setUp() throws IOException {
        File file = new File("src/test/resources/TestRTSAllUpdates");
        org.apache.commons.io.FileUtils.deleteDirectory(file);
    }

    @Test
    public void checkAnalyze() throws IOException, GitAPIException {
        File file = new File("src/test/resources/TestRTSAllUpdates");
        if (file.exists())
            org.apache.commons.io.FileUtils.deleteDirectory(file);

        File archive= new File("src/test/resources/TestRTSAllUpdates.zip");
        FileUtils.unzip(archive);

        assert file.exists() && file.isDirectory();

        Git git = Git.open(file);
        git.checkout().setCreateBranch(true).setName("tmp").setStartPoint("3bef257c73092096339491206835f9b3005715bc").call();
        App.main(new String[]{file.getAbsolutePath()});
        Git.open(file).checkout().setName("master").call();

        new GitCaller(file, file).compareCommits("3bef257c73092096339491206835f9b3005715bc", "master");
    }

    @After
    public void tearDown() throws IOException {
        File file = new File("src/test/resources/TestRTSAllUpdates");
     //   org.apache.commons.io.FileUtils.deleteDirectory(file);
    }
}

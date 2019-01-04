package com.tblf;

import com.tblf.compare.GitCaller;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Objects;

public class TraceBasedTestSelectionRegressionTest {

    private File project;

    @Before
    public void setup() throws IOException {
        project = new File("src/test/resources/TestRTSAllUpdates");
        if (project.exists())
            FileUtils.deleteDirectory(project);

        com.tblf.utils.FileUtils.unzip(new File("src/test/resources/TestRTSAllUpdatesQueue.zip"));
    }


    @Test
    public void checkAnalyze() throws IOException, GitAPIException {

        Collection<String> testImpacted = new GitCaller(project, project).traceBasedCommitComparison("master~1", "master");
        testImpacted.removeIf(Objects::isNull);
        Assert.assertTrue(testImpacted.contains("com.tblf.AppTest$testOne"));
        Assert.assertTrue(testImpacted.contains("com.tblf.AppTest$testTwo"));
        Assert.assertTrue(testImpacted.contains("com.tblf.AppTest$testThree"));
        Assert.assertTrue(testImpacted.contains("com.tblf.AppTest$testFive"));
        Assert.assertTrue(testImpacted.contains("com.tblf.AppTest$testSix"));
        Assert.assertTrue(testImpacted.contains("com.tblf.AppTest$testSeven"));
    }

    @After
    public void tearDown() throws IOException {
     org.apache.commons.io.FileUtils.deleteDirectory(project);
    }
}


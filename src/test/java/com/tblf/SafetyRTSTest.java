package com.tblf;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

public class SafetyRTSTest {
    private File file;

    @Before
    public void setUp() throws IOException {
        file = new File("src/test/resources/TestSafety");
    }

    @Test
    public void checkAnalyze() throws IOException, GitAPIException {
        Collection<String> stringCollection = new GitCaller(file).compareCommits("master~1", "master");

        Assert.assertTrue(stringCollection.contains("root.TestApp$testAdded"));
        Assert.assertTrue(stringCollection.contains("root.TestApp$testFalse"));
        Assert.assertTrue(stringCollection.contains("root.TestApp$testInheritance"));
        Assert.assertTrue(stringCollection.contains("root.TestApp$testInheritance2"));
        Assert.assertTrue(stringCollection.contains("root.TestApp$testTrue"));
    }

    @After
    public void tearDown() throws IOException {
    }
}

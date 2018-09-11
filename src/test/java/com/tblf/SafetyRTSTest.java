package com.tblf;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

public class SafetyRTSTest {
    private File file;

    @Before
    public void setUp() throws IOException {
        file = new File("src/test/resources/TestSafety");
    }

    @Test
    public void checkAnalyze() throws IOException, GitAPIException {
        new GitCaller(file).compareCommits("master~1", "master");
    }

    @After
    public void tearDown() throws IOException {
    }
}

package com.tblf;

import com.tblf.compare.GitCaller;
import com.tblf.utils.FileUtils;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

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

        Collection<String> testImpacted = new GitCaller(file, file).compareCommits("master~1", "master");
        testImpacted.forEach(System.out::println);
    }

    @After
    public void tearDown() throws IOException {
        File file = new File("src/test/resources/TestRTSAllUpdates");
        org.apache.commons.io.FileUtils.deleteDirectory(file);
    }
}

package com.tblf;

import com.tblf.compare.TraceBasedRegressionTestSelection;
import com.tblf.utils.FileUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

public class ImpactAnalysisTest {
    private File project;

    @Before
    public void setUp() throws IOException {
        FileUtils.unzip(new File("src/test/resources/TestImpactAnalysis.zip"));
        project = new File("src/test/resources/TestImpactAnalysis");
    }

    @Test
    public void test() {
        new App().buildImpactAnalysisModel(project);
        TraceBasedRegressionTestSelection traceBasedTestSelection = new TraceBasedRegressionTestSelection(project, null, null, null, null);
        Collection<String> stringCollection = traceBasedTestSelection.getAllImpactedTest("com.tblf.App$method");
        Assert.assertTrue(stringCollection.size() == 4);
    }

    @After
    public void tearDown() throws IOException {
        org.apache.commons.io.FileUtils.deleteDirectory(project);
    }
}

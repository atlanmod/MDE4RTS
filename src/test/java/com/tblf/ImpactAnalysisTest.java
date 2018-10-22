package com.tblf;

import com.tblf.utils.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

public class ImpactAnalysisTest {
    private File project;

    @Before
    public void setUp() throws IOException {
        FileUtils.unzip(new File("src/test/resources/TestImpactAnalysis.zip"));
        project = new File("src/test/resources/TestImpactAnalysis");
    }

    @Test
    public void test() {
        App.main(new String[]{project.getAbsolutePath()});
    }


    @After
    public void tearDown() {

    }
}

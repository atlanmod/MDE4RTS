package com.tblf;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

public class ImpactAnalysisTest {
    private File project;

    @Before
    public void setUp() {
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

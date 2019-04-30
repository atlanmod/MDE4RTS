package com.tblf;

import com.tblf.utils.FileUtils;
import org.junit.*;

import java.io.File;
import java.io.IOException;

@Ignore
public class DiscovererTest {
    private File project;

    @Before
    public void setUp() throws IOException {
        FileUtils.unzip(new File("src/test/resources/TestImpactAnalysis.zip"));
        project = new File("src/test/resources/TestImpactAnalysis");
        Assert.assertTrue(project.exists());
        new File(project, "TestImpactAnalysis_java.xmi").delete();

    }

    @Test
    public void testDiscover() {
        Assert.assertFalse(new File(project, "TestImpactAnalysis_java.xmi").exists());
        new Discoverer(project).run();
        Assert.assertTrue(new File(project, "TestImpactAnalysis_java.xmi").exists());
    }

    @After
    public void tearDown() throws IOException {
        org.apache.commons.io.FileUtils.deleteDirectory(project);
    }


}

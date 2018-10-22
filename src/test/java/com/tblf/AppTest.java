package com.tblf;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;

/**
 * Unit test for simple App.
 */
public class AppTest 
{

    @Before
    public void setUp() throws IOException {

    }

    @Test
    public void checkMain() throws IOException, URISyntaxException {
        App.main(new String[]{"src/test/resources/TestImpactAnalysis"});
    }

    @After
    public void tearDown() throws IOException {
        //org.apache.commons.io.FileUtils.deleteDirectory(directory);
    }
}

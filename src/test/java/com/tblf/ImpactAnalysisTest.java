package com.tblf;

import com.tblf.compare.TraceBasedRegressionTestSelection;
import com.tblf.utils.FileUtils;
import net.openhft.chronicle.queue.ChronicleQueue;
import net.openhft.chronicle.queue.ExcerptTailer;
import net.openhft.chronicle.queue.impl.single.SingleChronicleQueueBuilder;
import net.openhft.chronicle.wire.Wire;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mapdb.Queues;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;

public class ImpactAnalysisTest {
    private File project;

    @Before
    public void setUp() throws IOException {
        FileUtils.unzip(new File("src/test/resources/TestImpactAnalysis.zip"));
        project = new File("src/test/resources/TestImpactAnalysis");
    }

    @Test
    public void test() throws IOException {
        new App().buildImpactAnalysisModel(project);
        TraceBasedRegressionTestSelection traceBasedTestSelection = new TraceBasedRegressionTestSelection(project, null, null, null, null);
        Collection<String> stringCollection = traceBasedTestSelection.getAllTests();

        System.out.println(stringCollection);

    }

    @After
    public void tearDown() throws IOException {
        org.apache.commons.io.FileUtils.deleteDirectory(project);
    }
}

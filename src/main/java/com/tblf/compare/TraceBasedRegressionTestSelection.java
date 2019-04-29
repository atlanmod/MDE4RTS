package com.tblf.compare;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.nodeTypes.NodeWithName;
import com.tblf.parsingbehaviors.ParsingUtils;
import com.tblf.utils.Configuration;
import net.openhft.chronicle.queue.ChronicleQueue;
import net.openhft.chronicle.queue.ExcerptTailer;
import net.openhft.chronicle.queue.impl.single.SingleChronicleQueueBuilder;
import net.openhft.chronicle.wire.Wire;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.lib.ObjectId;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Level;

/**
 * Select test method impacted by a sut modification only by reading the corresponding topic in the traces
 */
public class TraceBasedRegressionTestSelection extends RegressionTestSelection {

    private File trace;
    private ChronicleQueue queue;

    /**
     * Constructor
     *
     * @param folder      the {@link File} directory that contains the .git repository
     * @param pomFolder   the {@link File} directory that contains the pom.xml file
     * @param diffFormat  a {@link DiffFormatter}
     * @param diffEntries
     * @param oldId       the {@link ObjectId} of the old revision
     */
    public TraceBasedRegressionTestSelection(File folder, File pomFolder, DiffFormatter diffFormat, List<DiffEntry> diffEntries, ObjectId oldId) {
        super(folder, pomFolder, diffFormat, diffEntries, oldId);
        this.trace = new File(folder, "trace");
        this.queue = SingleChronicleQueueBuilder.single(trace).build();
    }

    /**
     * read all the text in the topic given by the {@link String}
     * @param qualifiedName the {@link String}
     * @return a {@link Collection} of {@link String}
     */
    public Collection<String> getAllImpactedTest(String qualifiedName) {
        ExcerptTailer excerptTailer = queue.createTailer().toStart();
        Wire wire;
        Collection<String> impactedTests = new HashSet<>();
        while ((wire = excerptTailer.readingDocument().wire()) != null) {
            impactedTests.add(wire.read(qualifiedName).text());
        }

        return impactedTests;
    }

    /**
     * read all the text in the queue, in every topic.
     * @return
     */
    public Collection<String> getAllTests() {
        ExcerptTailer excerptTailer = queue.createTailer();
        Wire wire;
        Collection<String> tests = new HashSet<>();

        while ((wire = excerptTailer.readingDocument().wire()) != null) {
            tests.add(wire.read().text());
        }

        return tests;
    }

    /**
     * Read all the topics
     * @return
     */
    public Collection<String> getAllMethods() {
        ExcerptTailer excerptTailer = queue.createTailer();
        Wire wire;
        Collection<String> methods = new HashSet<>();

        while ((wire = excerptTailer.readingDocument().wire()) != null) {
            wire.readMap().keySet().forEach(System.out::println);
        }

        return methods;
    }

    @Override
    Collection<String> getImpactsOfMethodUpdate(MethodDeclaration methodDeclaration) {
        LOGGER.log(Level.INFO, ParsingUtils.getQualifiedName(methodDeclaration) + " update impacts computed");
        return (ParsingUtils.isTest(methodDeclaration)) ? Collections.singletonList(ParsingUtils.getQualifiedName(methodDeclaration)) : getAllImpactedTest(ParsingUtils.getQualifiedName(methodDeclaration));
    }

    @Override
    Collection<String> getImpactsOfMethodDeletion(MethodDeclaration methodDeclaration) {
        LOGGER.log(Level.INFO, ParsingUtils.getQualifiedName(methodDeclaration) + " deletion impacts computed");
        return (ParsingUtils.isTest(methodDeclaration)) ? Collections.emptyList() : getAllImpactedTest(ParsingUtils.getQualifiedName(methodDeclaration));
    }

    @Override
    Collection<String> getImpactsOfMethodAddition(MethodDeclaration methodDeclaration) {
        LOGGER.log(Level.INFO, ParsingUtils.getQualifiedName(methodDeclaration) + " deletion impacts computed");
        //is a test: We return the method directly
        if (methodDeclaration.getAnnotations().stream().map(NodeWithName::getNameAsString).anyMatch("Test"::equals)) {
            return Collections.singletonList(ParsingUtils.getQualifiedName(methodDeclaration));
        }

        if (methodDeclaration.getAnnotations().stream().map(NodeWithName::getNameAsString).anyMatch("Override"::equals)) {
            //TODO GET IMPACTS OF "+ ParsingUtils.getQualifiedName(methodDeclaration).toUpperCase()+" AT PARENT CLASS LEVEL");
            String methodOverridenQualifiedName = ParsingUtils.getMethodOverriden(methodDeclaration, gitFolder);
            return getAllImpactedTest(methodOverridenQualifiedName);
        }

        return Collections.emptyList();
    }


}

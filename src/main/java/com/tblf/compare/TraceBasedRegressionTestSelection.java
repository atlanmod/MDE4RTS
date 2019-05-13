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
import org.mapdb.DB;
import org.mapdb.DBMaker;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * Select test method impacted by a sut modification only by reading the corresponding topic in the traces
 */
public class TraceBasedRegressionTestSelection extends RegressionTestSelection {

    private File trace;
    private DB db;
    private ConcurrentMap<String, HashSet<String>> map;
    private static HashSet<String> EMPTY_SET = new HashSet<>();
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
        this.db = DBMaker.fileDB(this.trace).checksumHeaderBypass().fileMmapEnable().make();
        this.map = (ConcurrentMap<String, HashSet<String>>) this.db.hashMap("map").createOrOpen();
    }

    /**
     * read all the text in the topic given by the {@link String}
     * @param qualifiedName the {@link String}
     * @return a {@link Collection} of {@link String}
     */
    public Collection<String> getAllImpactedTest(String qualifiedName) {
        return this.map.getOrDefault(qualifiedName, EMPTY_SET);
    }

    /**
     * read all the text in the queue, in every topic.
     * @return
     */
    public Collection<String> getAllTests() {
        return this.map.values().stream().flatMap(Set::stream).collect(Collectors.toSet());
    }

    /**
     * Read all the keys
     * @return a {@link Collection} of {@link String}
     */
    public Collection<String> getAllMethods() {
        return this.map.keySet();
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

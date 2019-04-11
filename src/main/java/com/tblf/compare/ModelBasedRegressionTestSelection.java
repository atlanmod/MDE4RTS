package com.tblf.compare;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.tblf.parsing.indexer.HawkQuery;
import com.tblf.parsingbehaviors.EOLQueryBuilder;
import com.tblf.parsingbehaviors.ParsingUtils;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.lib.ObjectId;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Level;

public class ModelBasedRegressionTestSelection extends RegressionTestSelection {
    private EOLQueryBuilder eolQueryBuilder;
    private HawkQuery hawkQuery;

    /**
     * Constructor
     *
     * @param folder      the {@link File} directory that contains the .git repository
     * @param pomFolder   the {@link File} directory that contains the pom.xml file
     * @param diffFormat  a {@link DiffFormatter}
     * @param diffEntries
     * @param oldId       the {@link ObjectId} of the old revision
     */
    public ModelBasedRegressionTestSelection(File folder, File pomFolder, DiffFormatter diffFormat, List<DiffEntry> diffEntries, ObjectId oldId) {
        super(folder, pomFolder, diffFormat, diffEntries, oldId);
        this.eolQueryBuilder = new EOLQueryBuilder();
        this.hawkQuery = new HawkQuery(pomFolder);
    }

    @Override
    public Collection<String> getImpactsOfMethodUpdate(MethodDeclaration methodDeclaration) {
        LOGGER.log(Level.INFO, ParsingUtils.getQualifiedName(methodDeclaration) + " update impacts computed");
        return (Collection<String>) hawkQuery.queryWithInputEOLQuery(eolQueryBuilder.createGetImpactOfSingleMethodUpdate(methodDeclaration));
    }

    @Override
    public Collection<String> getImpactsOfMethodDeletion(MethodDeclaration methodDeclaration) {
        LOGGER.log(Level.INFO, ParsingUtils.getQualifiedName(methodDeclaration) + " deletion impacts computed");
        return (Collection<String>) hawkQuery.queryWithInputEOLQuery(eolQueryBuilder.createGetImpactOfSingleMethodUpdate(methodDeclaration));
    }

    @Override
    public Collection<String> getImpactsOfMethodAddition(MethodDeclaration methodDeclaration) {

        LOGGER.log(Level.INFO, ParsingUtils.getQualifiedName(methodDeclaration) + " addition impacts computed");
        Collection<String> impacts = new HashSet<>();
        methodDeclaration.getAnnotations().forEach(annotationExpr -> {
            if ("Test".equals(annotationExpr.getNameAsString())) {
                impacts.add(ParsingUtils.getQualifiedName(methodDeclaration));
            } else
            if ("Override".equals(annotationExpr.getNameAsString())) {
                impacts.addAll((Collection<String>) hawkQuery.queryWithInputEOLQuery(eolQueryBuilder.createGetImpactOfSingleMethodAddition(methodDeclaration)));
            }
        });

        return impacts;
    }
}

package com.tblf;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.google.common.collect.Range;
import com.tblf.gitdiff.NonJavaFileException;
import com.tblf.gitdiff.RangeFactory;
import com.tblf.parsing.indexer.HawkQuery;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.patch.FileHeader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class RegressionTestSelection {

    private static final RangeFactory<Integer> RANGE_FACTORY = new RangeFactory<>();

    private HawkQuery hawkQuery;
    private DiffFormatter diffFormatter;
    private File gitFolder;
    private EOLQueryBuilder eolQueryBuilder;

    private Collection<DiffEntry> diffEntryCollection;
    private Collection<String> testImpacted;

    private static final Logger LOGGER = Logger.getLogger(RegressionTestSelection.class.getName());


    public RegressionTestSelection(File folder, File pomFolder, List<DiffEntry> diffEntryList, DiffFormatter diffFormat) {
        diffEntryList.forEach(diffEntry -> LOGGER.info(String.valueOf(diffEntry.getChangeType())));

        gitFolder = folder;
        hawkQuery = new HawkQuery(pomFolder);
        diffFormatter = diffFormat;
        diffEntryCollection = diffEntryList;
        testImpacted = new ArrayList<>();
        eolQueryBuilder = new EOLQueryBuilder();
    }



























    public Collection<String> analyse() {
        diffEntryCollection.forEach(diffEntry -> {

            try {

                // The file is not a Java File.
                if (!diffEntry.getNewPath().equals("/dev/null") && !diffEntry.getNewPath().endsWith(".java"))
                    throw new NonJavaFileException("The diff entry: " + diffEntry.getNewPath() + " does not concern a Java file");

                if (diffEntry.getOldPath().equals("/dev/null")) {
                    LOGGER.info("File added in the current revision: " + diffEntry.getNewPath());
                    testImpacted.addAll(
                            manageNewFile(diffEntry) // The file is new
                    );

                } else {

                    LOGGER.info("File updated in the current revision: " + diffEntry.getOldPath() + " " + diffEntry.getChangeType());
                    testImpacted.addAll(
                            manageUpdatedFile(diffEntry) // The file has been updated
                    );
                }

                //hawkQuery.close();
            } catch (Exception e) {
                LOGGER.log(Level.INFO, "Could not analyze the diffEntry", e);
            }
        });
        testImpacted.forEach(s -> {
            LOGGER.info(s + " has been impacted by the changes");
        });
        return null;
    }

    /**
     * Analyse the {@link DiffEntry} of an existing file, and return the merged {@link Collection}s of Methods Qualified names
     *
     * @param diffEntry a {@link DiffEntry}
     * @return a {@link Collection} of {@link String}
     * @throws IOException
     */
    private Collection<? extends String> manageUpdatedFile(DiffEntry diffEntry) throws IOException {
        FileHeader fileHeader = diffFormatter.toFileHeader(diffEntry);

        File file = new File(gitFolder, diffEntry.getOldPath());

        if (!file.exists()) {
            throw new IOException(file.getAbsolutePath() + " does not exist");
        }

        final Collection<String> impacts = new ArrayList<>();

        fileHeader.toEditList().forEach(edit -> {
            if (edit.getType().equals(Edit.Type.REPLACE))
                impacts.addAll(manageReplace(edit, file));
            else if (edit.getType().equals(Edit.Type.DELETE))
                impacts.addAll(manageDeletion(edit, file));
        });

        //FIXME
        return impacts;
    }

    private Collection<? extends String> manageDeletion(Edit edit, File file) {
        try {
            CompilationUnit compilationUnit = JavaParser.parse(file);
            Collection<ClassOrInterfaceDeclaration> classOrInterfaceDeclarations = compilationUnit.getChildNodesByType(ClassOrInterfaceDeclaration.class);
            classOrInterfaceDeclarations.forEach(classOrInterfaceDeclaration -> {
                String className = classOrInterfaceDeclaration.getNameAsString(); //fixme use Qualified name here
                String eolQuery = eolQueryBuilder.createGetClassUsingNameQuery(className);
                Object value = hawkQuery.queryWithInputEOLQuery(eolQuery);
                System.out.println(value);
            });

        } catch (FileNotFoundException e) {
            LOGGER.log(Level.WARNING, "Could not parse the file " + file.getPath(), e);
        }

        return Collections.EMPTY_LIST; //Fixme
    }

    private Collection<String> manageReplace(Edit edit, File fileUpdated) {
        Collection<MethodDeclaration> methodDeclarations = getMethodDeclarationsEdited(fileUpdated, RANGE_FACTORY.createOpenOrSingletonRange(edit.getBeginB(), edit.getEndB()));

        System.out.println("EDIT: " + edit.getType());
        //FIXME This method is executed for each edit, and is quite consuming.
        //FIXME factorize it in order to execute it only once

        String eolQuery = eolQueryBuilder.createGetModifiedMethodQuery(methodDeclarations);
        Object value = hawkQuery.queryWithInputEOLQuery(eolQuery);

        return (Collection<String>) value;
    }


    /**
     * Get a diffEntry of a new file added in the previous commit and analyze it
     *
     * @param diffEntry a {@link DiffEntry}
     * @return a {@link Collection} of Test names as {@link String} to run
     * @throws IOException
     */
    private Collection<? extends String> manageNewFile(DiffEntry diffEntry) throws IOException {
        File file = new File(gitFolder, diffEntry.getNewPath());

        if (!file.exists()) {
            throw new IOException(file.getAbsolutePath() + " does not exist");
        }

        CompilationUnit compilationUnit = JavaParser.parse(file);
        Collection<MethodDeclaration> methodDeclarations = compilationUnit.getChildNodesByType(MethodDeclaration.class);

        return methodDeclarations.stream()
                .filter(methodDeclaration -> methodDeclaration.getAnnotations().stream()
                        .anyMatch(annotationExpr -> annotationExpr.getName().asString().equals("Test"))
                )
                .map(methodDeclaration -> methodDeclaration.getNameAsString())
                .collect(Collectors.toList());

        //FIXME: map to qualified name, and not only method name
    }

    /**
     * Parse the file edited in the {@link DiffEntry} and returns all the method that have been edited
     *
     * @param @{@link File} the file updated
     * @param range   an {@link Edit} modified set of lines as a {@link Range}, considering a set of updated lines of code
     * @return a {@link Collection} of {@link MethodDeclaration}s
     */
    private Collection<MethodDeclaration> getMethodDeclarationsEdited(File file, Range<Integer> range) {
        Collection<MethodDeclaration> methodDeclarations = new LinkedList<>();

        try {
            CompilationUnit compilationUnit = JavaParser.parse(file);
            LOGGER.info(String.valueOf(range));
            methodDeclarations.addAll(compilationUnit.getChildNodesByType(MethodDeclaration.class).stream()
                    .peek(methodDeclaration -> LOGGER.info(methodDeclaration.getNameAsString() + " " + methodDeclaration.getRange().get().begin + " -> " + methodDeclaration.getRange().get().end))
                    .filter(methodDeclaration -> methodDeclaration.getRange().isPresent())
                    .filter(methodDeclaration -> range.isConnected(RANGE_FACTORY.createOpenOrSingletonRange(methodDeclaration.getRange().get().begin.line, methodDeclaration.getRange().get().end.line)))
                    .collect(Collectors.toList()));
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Could not parse the file " + file.getAbsolutePath(), e);
        }

        methodDeclarations.forEach(methodDeclaration -> {
            LOGGER.info(methodDeclaration.getNameAsString() + " has been edited ");
        });
        return methodDeclarations;
    }
}

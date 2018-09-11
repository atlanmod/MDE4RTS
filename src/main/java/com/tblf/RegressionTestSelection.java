package com.tblf;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.tblf.gitdiff.NonJavaFileException;
import com.tblf.gitdiff.RangeFactory;
import com.tblf.parsing.indexer.HawkQuery;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.Edit;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class RegressionTestSelection {

    private static final RangeFactory<Integer> RANGE_FACTORY = new RangeFactory<>();

    private Collection<?> newMethods;
    private Collection<?> updatedMethods;
    private Collection<?> deletedMethods;

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



    public Collection<String> getAllMethodsImpacted() {
        diffEntryCollection.forEach(diffEntry -> {

            try {

                // The file is not a Java File.
                if (!diffEntry.getNewPath().equals("/dev/null") && !diffEntry.getNewPath().endsWith(".java"))
                    throw new NonJavaFileException("The diff entry: " + diffEntry.getNewPath() + " does not concern a Java file");

                if (diffEntry.getOldPath().equals("/dev/null")) {
                    LOGGER.info("File added in the current revision: " + diffEntry.getNewPath());
                    testImpacted.addAll(manageNewFile(diffEntry)); // The file is new
                } else {
                    LOGGER.info("File updated in the current revision: " + diffEntry.getOldPath() + " " + diffEntry.getChangeType());
                    identifyModificationType(diffEntry);
                }



            } catch (Exception e) {
                LOGGER.log(Level.INFO, "Could not analyze the diffEntry", e);
            }
        });

        testImpacted.forEach(s -> {
            LOGGER.info(s + " has been impacted by the changes");
        });
        return null;
    }

    private void identifyModificationType(DiffEntry diffEntry) throws IOException {
        diffFormatter.toFileHeader(diffEntry).toEditList().forEach(edit -> {
            switch(edit.getType()) {
                case DELETE:
                    manageDeleteEdition(edit, diffEntry);
                    break;
                case REPLACE:
                    manageReplaceEdition(edit, diffEntry);
                    break;
                case INSERT:
                    manageInsertEdition(edit, diffEntry);
                    break;
                case EMPTY:

            }
        });
    }

    private void manageReplaceEdition(Edit edit, DiffEntry diffEntry) {
        File updatedFile = new File(diffEntry.getOldPath());
        try {
            CompilationUnit compilationUnit = JavaParser.parse(updatedFile);
            

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void manageDeleteEdition(Edit edit, DiffEntry diffEntry) {
    }

    private void manageInsertEdition(Edit edit, DiffEntry diffEntry) {
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
}

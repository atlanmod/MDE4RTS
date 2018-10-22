package com.tblf;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.tblf.parsing.indexer.HawkQuery;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class RegressionTestSelection {

    private Collection<MethodDeclaration> newMethods;
    private Collection<MethodDeclaration> updatedMethods;
    private Collection<MethodDeclaration> deletedMethods;

    private Map<File, CompilationUnit> filesJavaParsedNewRevision;
    private Map<File, CompilationUnit> filesJavaParsedOldRevision;

    private HawkQuery hawkQuery;
    private DiffFormatter diffFormatter;
    private File gitFolder;
    private EOLQueryBuilder eolQueryBuilder;

    private Collection<String> testImpacted;

    private ObjectId oldId;
    private ObjectId newId;

    private static final Logger LOGGER = Logger.getLogger(RegressionTestSelection.class.getName());


    /**
     * Constructor
     * @param folder the {@link File} directory that contains the .git repository
     * @param pomFolder the {@link File} directory that contains the pom.xml file
     * @param diffFormat a {@link DiffFormatter}
     * @param oldId the {@link ObjectId} of the old revision
     * @param newId the {@link ObjectId} of the new revision
     */
    public RegressionTestSelection(File folder, File pomFolder, DiffFormatter diffFormat, ObjectId oldId, ObjectId newId) {

        this.gitFolder = folder;
        this.diffFormatter = diffFormat;
        this.oldId = oldId;
        this.newId = newId;

        this.hawkQuery = new HawkQuery(pomFolder);

        this.testImpacted = new HashSet<>();
        this.eolQueryBuilder = new EOLQueryBuilder();

        this.filesJavaParsedNewRevision = new HashMap<>();
        this.filesJavaParsedOldRevision = new HashMap<>();

        this.newMethods = new ArrayList<>();
        this.updatedMethods = new ArrayList<>();
        this.deletedMethods = new ArrayList<>();
    }

    /**
     * Get all the methods impacted by modifications between two revisions
     *
     * @return a {@link Collection} of method qualified names as {@link String}
     */
    @SuppressWarnings("unchecked")
    public Collection<String> getAllMethodsImpacted() {

        Collection<DiffEntry> diffEntryCollection;
        try {
            diffEntryCollection = diffFormatter.scan(oldId, newId);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Couldn't run the diff", e);
            diffEntryCollection = new ArrayList<>();
        }

        diffEntryCollection.forEach(diffEntry -> {
            try {
                // The file is not a Java File.
                if (diffEntry.getOldPath().equals("/dev/null") && diffEntry.getNewPath().endsWith(".java")) {
                    LOGGER.info("File added in the oldId revision: " + diffEntry.getNewPath());
                    testImpacted.addAll(manageNewFile(diffEntry)); // The file is new
                } else if (diffEntry.getNewPath().equals("/dev/null") && diffEntry.getOldPath().endsWith(".java")) {
                    LOGGER.info("File deleted in the newId revision: " + diffEntry.getOldPath());
                    testImpacted.addAll(manageDeletedFile(diffEntry));
                } else if (diffEntry.getNewPath().endsWith(".java")){
                    identifyModificationType(diffEntry);
                }
            } catch (Exception e) {
                LOGGER.log(Level.INFO, "Could not analyze the diffEntry: ", e);
            }
        });

        deletedMethods.forEach(methodDeclaration -> {
            LOGGER.log(Level.INFO, eolQueryBuilder.getQualifiedName(methodDeclaration) + " deletion impacts computed");
            testImpacted.addAll((Collection<? extends String>) hawkQuery.queryWithInputEOLQuery(eolQueryBuilder.createGetImpactOfSingleMethodUpdate(methodDeclaration)));
        });

        updatedMethods.forEach(methodDeclaration -> {
            LOGGER.log(Level.INFO, eolQueryBuilder.getQualifiedName(methodDeclaration) + " update impacts computed");
            testImpacted.addAll((Collection<? extends String>) hawkQuery.queryWithInputEOLQuery(eolQueryBuilder.createGetImpactOfSingleMethodUpdate(methodDeclaration)));
        });

        newMethods.forEach(methodDeclaration -> {
            LOGGER.log(Level.INFO, eolQueryBuilder.getQualifiedName(methodDeclaration) + " addition impacts computed");
            methodDeclaration.getAnnotations().forEach(annotationExpr -> {
                if ("Test".equals(annotationExpr.getNameAsString())) {
                    testImpacted.add(eolQueryBuilder.getQualifiedName(methodDeclaration));
                }
                if ("Override".equals(annotationExpr.getNameAsString())) {
                    testImpacted.addAll((Collection<? extends String>) hawkQuery.queryWithInputEOLQuery(eolQueryBuilder.createGetImpactOfSingleMethodAddition(methodDeclaration)));
                }
            });
        });

        try {
            hawkQuery.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return testImpacted;
    }

    /**
     * Compare two files that has been updated in a diffentry and sort the MethodDeclarations by modification types
     *
     * @param diffEntry the {@link DiffEntry} considering the oldPath and the revision information
     * @throws IOException if the {@link DiffEntry} cannot be analyzed
     */
    private void identifyModificationType(DiffEntry diffEntry) throws IOException {
        Map<String, MethodDeclaration> newFileMethods = getOrParseFileNewRev(diffEntry).getChildNodesByType(MethodDeclaration.class).stream().collect(Collectors.toMap(o -> eolQueryBuilder.getQualifiedName(o), o -> o));
        Map<String, MethodDeclaration> oldFileMethods = getOrParseFileOldRev(diffEntry).getChildNodesByType(MethodDeclaration.class).stream().collect(Collectors.toMap(o -> eolQueryBuilder.getQualifiedName(o), o -> o));

        newFileMethods.forEach((s, newMethod) -> {
            MethodDeclaration oldMethod = oldFileMethods.get(s);
            //The method does not exist in the previous revision, so it's new
            if (oldMethod == null) {
                newMethods.add(newMethod);
            } else if (oldMethod.toString().length() != newMethod.toString().length() || !oldMethod.toString().equals(newMethod.toString())) {
                //The method is different in the new revision. it has been updated
                updatedMethods.add(newMethod);
            }
        });

        oldFileMethods.forEach((s, oldMethod) -> {
            MethodDeclaration newMethod = newFileMethods.get(s);
            //The method does not exist in the next revision, so it has been deleted
            if (newMethod == null) {
                deletedMethods.add(oldMethod);
            }
        });
    }

    /**
     * Get a diffEntry of a new file added in the previous commit and analyze it
     *
     * @param diffEntry a {@link DiffEntry}
     * @return a {@link Collection} of Test names as {@link String} to run
     * @throws IOException if the new file cannot be found or parsed
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
                .map(methodDeclaration -> eolQueryBuilder.getQualifiedName(methodDeclaration))
                .collect(Collectors.toList());
    }

    /**
     * Parses a file that has been deleted and add all the method deleted in the list of deleted methods for impact analysis
     *
     * @param diffEntry a {@link DiffEntry}
     * @return a {@link Collection} of {@link String} qualified names
     */
    private Collection<? extends String> manageDeletedFile(DiffEntry diffEntry) {
        String fileContent = getOldFileContent(diffEntry);
        CompilationUnit compilationUnit = JavaParser.parse(fileContent);
        return compilationUnit.getChildNodesByType(MethodDeclaration.class).stream().map(methodDeclaration -> eolQueryBuilder.getQualifiedName(methodDeclaration)).collect(Collectors.toList());
    }

    /**
     * Takes a file, and either parse it with {@link JavaParser} if the file hasnt been parsed yet
     * Or gets it in a hashmap
     *
     * @param diffEntry the {@link DiffEntry} considering the newPath and the revision information
     * @return the {@link CompilationUnit}
     */

    private CompilationUnit getOrParseFileNewRev(DiffEntry diffEntry) {
        File file = new File(gitFolder, diffEntry.getNewPath());
        CompilationUnit compilationUnit;

        if (filesJavaParsedNewRevision.containsKey(file))
            compilationUnit = filesJavaParsedNewRevision.get(file);
        else {
            try {
                compilationUnit = JavaParser.parse(file);
                filesJavaParsedNewRevision.put(file, compilationUnit);
            } catch (FileNotFoundException e) {
                LOGGER.log(Level.WARNING, "Could not find the file " + file.getAbsolutePath(), e);
                compilationUnit = new CompilationUnit();
            }
        }
        return compilationUnit;
    }

    /**
     * Takes a file, and either parse it with {@link JavaParser} if the file hasnt been parsed yet
     * Or gets it in a hashmap
     *
     * @param diffEntry the {@link DiffEntry} considering the oldPath and the revision information
     * @return the {@link CompilationUnit}
     */
    private CompilationUnit getOrParseFileOldRev(DiffEntry diffEntry) {
        CompilationUnit compilationUnit;
        File file = new File(gitFolder, diffEntry.getOldPath());

        if (filesJavaParsedOldRevision.containsKey(file))
            compilationUnit = filesJavaParsedOldRevision.get(file);
        else {
            String oldContent = getOldFileContent(diffEntry);
            compilationUnit = JavaParser.parse(oldContent);
            filesJavaParsedOldRevision.put(file, compilationUnit);
        }

        return compilationUnit;
    }

    /**
     * Return the old version of a file considered by a {@link DiffEntry}
     * @param diffEntry A {@link DiffEntry}
     * @return a {@link String}, the full content of the old file
     */
    private String getOldFileContent(DiffEntry diffEntry) {
        String path = diffEntry.getOldPath();
        try {
            Repository repository = Git.open(gitFolder).getRepository();

            ObjectReader objectReader = repository.newObjectReader();

            RevWalk revWalk = new RevWalk(objectReader);
            RevTree revTree = revWalk.parseCommit(oldId).getTree();
            TreeWalk treewalk = TreeWalk.forPath(objectReader, path, revTree);

            if (treewalk != null) {
                // use the blob id to read the file's data
                byte[] data = objectReader.open(treewalk.getObjectId(0)).getBytes();
                return new String(data, "utf-8");
            } else {
                throw new IOException("Couldn't open the old file");
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "An error happened when getting the old version of " + path, e);
            return "";
        }
    }
}

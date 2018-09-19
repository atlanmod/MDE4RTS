package com.tblf;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.google.common.collect.Range;
import com.tblf.gitdiff.NonJavaFileException;
import com.tblf.gitdiff.RangeFactory;
import com.tblf.parsing.indexer.HawkQuery;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.Edit;
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

    private static final RangeFactory<Integer> RANGE_FACTORY = new RangeFactory<>();

    private Collection<MethodDeclaration> newMethods;
    private Collection<MethodDeclaration> updatedMethods;
    private Collection<MethodDeclaration> deletedMethods;

    private Map<File, CompilationUnit> filesJavaParsedNewRevision;
    private Map<File, CompilationUnit> filesJavaParsedOldRevision;

    private HawkQuery hawkQuery;
    private DiffFormatter diffFormatter;
    private File gitFolder;
    private EOLQueryBuilder eolQueryBuilder;

    private Collection<DiffEntry> diffEntryCollection;
    private Collection<String> testImpacted;

    private ObjectId oldId;
    private ObjectId newId;

    private static final Logger LOGGER = Logger.getLogger(RegressionTestSelection.class.getName());


    public RegressionTestSelection(File folder, File pomFolder, DiffFormatter diffFormat, ObjectId oldId, ObjectId newId) {

        this.gitFolder = folder;
        this.diffFormatter = diffFormat;
        this.oldId = oldId;
        this.newId = newId;

        this.hawkQuery = new HawkQuery(pomFolder);

        this.testImpacted = new ArrayList<>();
        this.eolQueryBuilder = new EOLQueryBuilder();

        this.filesJavaParsedNewRevision = new HashMap<>();
        this.filesJavaParsedOldRevision = new HashMap<>();

        this.newMethods = new ArrayList<>();
        this.updatedMethods = new ArrayList<>();
        this.deletedMethods = new ArrayList<>();
    }


    public Collection<String> getAllMethodsImpacted() {
        hawkQuery.queryWithInputEOLQuery(eolQueryBuilder.createUpdateMethodDeclarationsWithQN());
        try {
            diffEntryCollection = diffFormatter.scan(oldId, newId);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Couldn't run the diff", e);
            diffEntryCollection = new ArrayList<>();
        }

        diffEntryCollection.forEach(diffEntry -> {
            try {

                // The file is not a Java File.
                if (!diffEntry.getNewPath().equals("/dev/null") && !diffEntry.getNewPath().endsWith(".java"))
                    throw new NonJavaFileException("The diff entry: " + diffEntry.getNewPath() + " does not concern a Java file");

                if (diffEntry.getOldPath().equals("/dev/null")) {
                    LOGGER.info("File added in the oldId revision: " + diffEntry.getNewPath());
                    testImpacted.addAll(manageNewFile(diffEntry)); // The file is new
                } else {
                    identifyModificationType(diffEntry);
                }
            } catch (Exception e) {
                LOGGER.log(Level.INFO, "Could not analyze the diffEntry", e);
            }
        });

        filterMethodModified();
        computeImpacts();
        
        testImpacted.forEach(s -> {
            LOGGER.info(s + " has been impacted by the changes");
        });

        try {
            hawkQuery.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Iterates over the selected methods, and computes their impacts according to the update done.
     */
    private void computeImpacts() {
        deletedMethods.forEach(methodDeclaration -> {
            Object o = hawkQuery.queryWithInputEOLQuery(eolQueryBuilder.createGetImpactOfSingleMethodUpdate(methodDeclaration));
            System.out.println("Methods selected by "+methodDeclaration.getNameAsString()+" deletion: "+o);
        });

        updatedMethods.forEach(methodDeclaration -> {
            Object o = hawkQuery.queryWithInputEOLQuery(eolQueryBuilder.createGetImpactOfSingleMethodUpdate(methodDeclaration));
            System.out.println("Methods selected by "+methodDeclaration.getNameAsString()+" update: "+o);
        });

        newMethods.forEach(methodDeclaration -> {
            Object o = hawkQuery.queryWithInputEOLQuery(eolQueryBuilder.createGetImpactOfSingleMethodAddition(methodDeclaration));
            System.out.println("Methods selected by "+methodDeclaration.getNameAsString()+" addition: "+o);
        });
    }

    /**
     * Iterates over a {@link DiffEntry} and manage each type of {@link Edit}
     *
     * @param diffEntry the {@link DiffEntry} considering the oldPath and the revision information
     * @throws IOException if the {@link DiffEntry} cannot be analyzed
     */
    private void identifyModificationType(DiffEntry diffEntry) throws IOException {
        diffFormatter.toFileHeader(diffEntry).toEditList().forEach(edit -> {
            switch (edit.getType()) {
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

    /**
     * Iterates over the methods impacted by replacement, deletion and addition, and swap them accross list in order to adapt the RTS
     * For instance: Lets consider a method that have been selected in the case of a deletion
     * If the method is still existing in the new code, then only *internal* lines of code have been deleted, and then the impacts
     * can be computed in a straightforward way, just like replacement. Hence the method will be removed from the list referencing the
     * deleted method, and put in the modified method list instead.
     * @warning Stream.removeAll() has a quadratic complexity. If the lists are large (more than 10 000) elements, using {@link Collectors}.partitioningBy
     * is more cost-efficient
     *
     */
    private void filterMethodModified() {
        Collection<MethodDeclaration> existingMethodWithCodeAdded = newMethods
                .stream()
                .filter(methodDeclaration -> (Boolean) hawkQuery.queryWithInputEOLQuery(eolQueryBuilder.createIsMethodExisting(methodDeclaration)))
                .collect(Collectors.toList());

        Collection<MethodDeclaration> newMethodAddedDuringCodeReplacement = updatedMethods
                .stream()
                .filter(methodDeclaration -> (Boolean) hawkQuery.queryWithInputEOLQuery(eolQueryBuilder.createIsMethodNotExisting(methodDeclaration)))
                .peek(methodDeclaration -> System.out.println("Method moved from existing to new "+methodDeclaration.getNameAsString()))
                .collect(Collectors.toList());

        updatedMethods.removeAll(newMethodAddedDuringCodeReplacement);
        newMethods.addAll(newMethodAddedDuringCodeReplacement);

        updatedMethods.addAll(existingMethodWithCodeAdded);
        newMethods.removeAll(existingMethodWithCodeAdded);
    }

    /**
     * Takes an {@link Edit} and gets all the methods modified by this edition
     *
     * @param edit      the {@link Edit}
     * @param diffEntry a {@link DiffEntry} used to linkn the {@link Edit} to a {@link File}
     */
    private void manageReplaceEdition(Edit edit, DiffEntry diffEntry) {
        Range<Integer> range = RANGE_FACTORY.createOpenOrSingletonRange(edit.getBeginB(), edit.getEndB());
        File updatedFile = new File(gitFolder, diffEntry.getOldPath());
        CompilationUnit compilationUnit = getOrParseFileNewRev(updatedFile);
        Collection<MethodDeclaration> methodDeclarations = computeModifiedMethodInCompilationUnit(range, compilationUnit);
        updatedMethods.addAll(methodDeclarations);
    }

    /**
     * Takes an {@link Edit} and get all the methods that have been impacted (deleted partially or totally) by this edition
     *
     * @param edit      the {@link Edit}
     * @param diffEntry the Corresponding {@link DiffEntry}
     */
    private void manageDeleteEdition(Edit edit, DiffEntry diffEntry) {
        Range<Integer> range = RANGE_FACTORY.createOpenOrSingletonRange(edit.getBeginA(), edit.getEndA());
        CompilationUnit compilationUnit = getOrParseFileOldRev(diffEntry);
        Collection<MethodDeclaration> methodDeclarations = computeModifiedMethodInCompilationUnit(range, compilationUnit);
        deletedMethods.addAll(methodDeclarations);
    }

    /**
     * Takes an {@link Edit} and a {@link DiffEntry} and g
     *
     * @param edit
     * @param diffEntry
     */
    private void manageInsertEdition(Edit edit, DiffEntry diffEntry) {
        Range<Integer> range = RANGE_FACTORY.createOpenOrSingletonRange(edit.getBeginB(), edit.getEndB());
        File updatedFile = new File(gitFolder, diffEntry.getNewPath());
        CompilationUnit compilationUnit = getOrParseFileNewRev(updatedFile);
        Collection<MethodDeclaration> methodDeclarations = computeModifiedMethodInCompilationUnit(range, compilationUnit);
        newMethods.addAll(methodDeclarations);
    }

    /**
     * Takes a {@link CompilationUnit}, a {@link Range} of lines of code updated, and returns all the impacted {@link MethodDeclaration}
     *
     * @param range           a {@link Range}
     * @param compilationUnit a {@link CompilationUnit}
     * @return a {@link Collection} or {@link MethodDeclaration}
     */
    private Collection<MethodDeclaration> computeModifiedMethodInCompilationUnit(Range range, CompilationUnit compilationUnit) {
        return compilationUnit.getChildNodesByType(MethodDeclaration.class).stream()
                .filter(methodDeclaration -> methodDeclaration.getRange().isPresent())
                .filter(methodDeclaration -> range.isConnected(RANGE_FACTORY.createOpenOrSingletonRange(methodDeclaration.getRange().get().begin.line, methodDeclaration.getRange().get().end.line)))
                .collect(Collectors.toList());
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
     * Takes a file, and either parse it with {@link JavaParser} if the file hasnt been parsed yet
     * Or gets it in a hashmap
     *
     * @param file the File to parse, or get
     * @return the {@link CompilationUnit}
     */
    private CompilationUnit getOrParseFileNewRev(File file) {
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
     * //FIXME This method is expensive, and should be not used with its oldId state.
     *
     * @param diffEntry A {@link DiffEntry}
     * @return a {@link String}, the full content of the old file
     * @throws IOException
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

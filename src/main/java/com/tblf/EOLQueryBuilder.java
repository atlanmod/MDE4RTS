package com.tblf;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

public class EOLQueryBuilder {

    public static final Logger LOGGER = Logger.getLogger("EOLQueryBuilder");

    public String createGetClassUsingNameQuery(String className) {
        String eolNameDeclaration = "var className = \""+className+"\";\n";

        try {
            return eolNameDeclaration.concat(FileUtils.readFileToString(new File("src/main/resources/queries/getClassUsingName.eol"), Charset.defaultCharset()));
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Couldn't build the EOL query");
        }

        return eolNameDeclaration;
    }

    public String createGetImpactOfSingleMethodUpdate(MethodDeclaration methodDeclaration) {
        String eolNameDeclaration = "var methodDeclaration = \"" + methodDeclaration.getNameAsString() + "\";\n";

        try {
            eolNameDeclaration = eolNameDeclaration.concat(FileUtils.readFileToString(new File("src/main/resources/queries/getMethodImpactedFromMethodUpdate.eol"), Charset.defaultCharset()));
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Couldn't build the EOL query");
        }

        return eolNameDeclaration;
    }

    public String createGetModifiedMethodsQuery(Collection<MethodDeclaration> methodDeclarations) {
        StringBuilder sequence = new StringBuilder("var methodModifiedNames = Sequence{");
        methodDeclarations.stream().forEach(methodDeclaration -> {
            ClassOrInterfaceDeclaration classOrInterfaceDeclaration = (ClassOrInterfaceDeclaration) methodDeclaration.getParentNode().get();

            sequence.append("\"" + classOrInterfaceDeclaration.getName().asString() + "$" + methodDeclaration.getNameAsString() + "\",");
        });

        if (sequence.lastIndexOf(",") == sequence.length() - 1) {
            sequence.deleteCharAt(sequence.length() - 1);
        }

        sequence.append("};\n");

        try {
            sequence.append(FileUtils.readFileToString(new File("src/main/resources/queries/getRootMethodImpacted.eol"), Charset.defaultCharset()));
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Couldn't build the EOL query");
        }

        return sequence.toString();
    }


    public String createGetImpactOfSingleMethodAddition(MethodDeclaration methodDeclaration) {
        String eolNameDeclaration = "var methodDeclaration = \"" + methodDeclaration.getNameAsString() + "\";\n";

        try {
            eolNameDeclaration = eolNameDeclaration.concat(FileUtils.readFileToString(new File("src/main/resources/queries/getMethodImpactedFromMethodAddition.eol"), Charset.defaultCharset()));
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Couldn't build the EOL query");
        }

        return eolNameDeclaration;
    }

    /**
     * Creates an EOL query that verify that a specific method declaration exist in the model
     * @param methodDeclaration a {@link MethodDeclaration} whom data will be used to query the model
     * @return a {@link String}, the EOL Query
     * //FIXME use the qualified name instead
     */
    public String createIsMethodExisting(MethodDeclaration methodDeclaration) {
        String qn = getQualifiedName(methodDeclaration);
        return "return MethodDeclaration.all.select( m | m.name == \""+methodDeclaration.getNameAsString()+"\");\n";
    }

    /**
     * Creates an EOL query that verify that a specific method declaration is not present in the model.
     * @param methodDeclaration a {@link MethodDeclaration}, used to query its equivalent in the model
     * @return a {@link String}, the EOL Query
     */
    public String createIsMethodNotExisting(MethodDeclaration methodDeclaration) {
        String qn = getQualifiedName(methodDeclaration);
        return "return MethodDeclaration.all.forAll(m | m.name <> \"" + methodDeclaration.getNameAsString() + "\");";
    }

    /**
     * Creates an EOL query that would update all the method declarations in the model with their respective qualified names
     * @return a {@link String}, the query
     */
    public String createUpdateMethodDeclarationsWithQN() {
        try {
            return FileUtils.readFileToString(new File("src/main/resources/queries/updateMethodDeclaration.eol"), Charset.defaultCharset());
        } catch (IOException e) {
            return "";
        }
    }

    /**
     * Iterates over the parents nodes of a class to get the qualified name
     * @param methodDeclaration a {@link MethodDeclaration}
     * @return a Qualified name as a {@link String}
     */
    public String getQualifiedName(MethodDeclaration methodDeclaration) {
        String QN = "";
        if (methodDeclaration.getParentNode().isPresent() && methodDeclaration.getParentNode().get().getClass().equals(ClassOrInterfaceDeclaration.class)) {
            ClassOrInterfaceDeclaration classOrInterfaceDeclaration = (ClassOrInterfaceDeclaration) methodDeclaration.getParentNode().get();
            CompilationUnit compilationUnit = classOrInterfaceDeclaration.getAncestorOfType(CompilationUnit.class).orElse(new CompilationUnit());
            if (compilationUnit.getPackageDeclaration().isPresent()) {
                QN = QN.concat(compilationUnit.getPackageDeclaration().get().getName()+".");
            }
            QN = QN + classOrInterfaceDeclaration.getName() + "$";
        }

        return QN+methodDeclaration.getNameAsString();
    }
}

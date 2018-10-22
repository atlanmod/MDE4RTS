package com.tblf;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.logging.Level;
import java.util.logging.Logger;

public class EOLQueryBuilder {

    public static final Logger LOGGER = Logger.getLogger("EOLQueryBuilder");

    public String createGetImpactOfSingleMethodUpdate(MethodDeclaration methodDeclaration) {
        String eolNameDeclaration = createQualifiedNameEolDeclaration(methodDeclaration);

        try {
            eolNameDeclaration = eolNameDeclaration.concat(FileUtils.readFileToString(new File("src/main/resources/queries/getMethodImpactedFromMethodUpdate.eol"), Charset.defaultCharset()));
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Couldn't build the EOL query");
        }

        return eolNameDeclaration;
    }

    public String createGetImpactOfSingleMethodAddition(MethodDeclaration methodDeclaration) {
        String eolNameDeclaration = createQualifiedNameEolDeclaration(methodDeclaration);

        try {
            eolNameDeclaration = eolNameDeclaration.concat(FileUtils.readFileToString(new File("src/main/resources/queries/getMethodImpactedFromMethodAddition.eol"), Charset.defaultCharset()));
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Couldn't build the EOL query");
        }

        return eolNameDeclaration;
    }

    /**
     * Creates the EOL Declaration of a method name and classname
     * @param methodDeclaration a {@link MethodDeclaration}
     * @return a String such as: "var methodDeclaration = methodDeclarationName; var classDeclaration = methodDeclarationClassName;"
     */
    public String createQualifiedNameEolDeclaration(MethodDeclaration methodDeclaration) {
        String eolNameDeclaration = "var methodDeclaration = \"" + methodDeclaration.getNameAsString() + "\";\n";
        return eolNameDeclaration.concat("var classDeclaration = \"" + ((ClassOrInterfaceDeclaration) methodDeclaration.getParentNode().orElse(new ClassOrInterfaceDeclaration())).getNameAsString() + "\";\n");
    }

    /**
     * Iterates over the parents nodes of a class to get the qualified name
     *
     * @param methodDeclaration a {@link MethodDeclaration}
     * @return a Qualified name as a {@link String}
     */
    public String getQualifiedName(MethodDeclaration methodDeclaration) {
        String QN = "";
        if (methodDeclaration.getParentNode().isPresent() && methodDeclaration.getParentNode().get().getClass().equals(ClassOrInterfaceDeclaration.class)) {
            ClassOrInterfaceDeclaration classOrInterfaceDeclaration = (ClassOrInterfaceDeclaration) methodDeclaration.getParentNode().get();
            CompilationUnit compilationUnit = classOrInterfaceDeclaration.getAncestorOfType(CompilationUnit.class).orElse(new CompilationUnit());
            if (compilationUnit.getPackageDeclaration().isPresent()) {
                QN = QN.concat(compilationUnit.getPackageDeclaration().get().getName() + ".");
            }
            QN = QN + classOrInterfaceDeclaration.getName() + "$";
        }

        return QN + methodDeclaration.getNameAsString();
    }
}

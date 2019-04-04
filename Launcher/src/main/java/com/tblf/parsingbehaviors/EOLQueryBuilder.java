package com.tblf.parsingbehaviors;

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
        String methodQualifiedName = "var methodQualifiedName = \""+ParsingUtils.getQualifiedName(methodDeclaration)+"\";\n";
        String eolMethodNameDeclaration = "var methodDeclaration = \"" + methodDeclaration.getNameAsString() + "\";\n";
        String eolClassNameDeclaration = "var classDeclaration = \"" + ((ClassOrInterfaceDeclaration) methodDeclaration.getParentNode().orElse(new ClassOrInterfaceDeclaration())).getNameAsString() + "\";\n";
        return methodQualifiedName.concat(eolMethodNameDeclaration).concat(eolClassNameDeclaration);
    }
}

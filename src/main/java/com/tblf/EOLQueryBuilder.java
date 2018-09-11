package com.tblf;

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

    public String createGetModifiedMethodQuery(Collection<MethodDeclaration> methodDeclarations) {
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
}

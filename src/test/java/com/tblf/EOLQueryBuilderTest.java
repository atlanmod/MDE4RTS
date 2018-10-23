package com.tblf;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.tblf.parsingbehaviors.EOLQueryBuilder;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.concurrent.atomic.AtomicInteger;

public class EOLQueryBuilderTest {

    private EOLQueryBuilder eolQueryBuilder;

    @Before
    public void setUp() {
        eolQueryBuilder = new EOLQueryBuilder();
    }

    @Test
    public void checkQetQualifiedNameWithImportAndClass() throws FileNotFoundException {
        final AtomicInteger atomicInteger = new AtomicInteger(0);
        CompilationUnit compilationUnit = JavaParser.parse(new File("src/test/resources/classes/App.java"));
        compilationUnit.getChildNodesByType(MethodDeclaration.class).forEach(methodDeclaration -> {
            atomicInteger.incrementAndGet();
            String methodName = methodDeclaration.getNameAsString();
            Assert.assertEquals("root.App$"+methodName, eolQueryBuilder.getQualifiedName(methodDeclaration));
        });

        Assert.assertEquals(2, atomicInteger.get());
    }
}

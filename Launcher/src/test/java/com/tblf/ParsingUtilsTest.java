package com.tblf;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.tblf.parsingbehaviors.ParsingUtils;
import com.tblf.utils.ModelUtils;
import org.eclipse.gmt.modisco.java.*;
import org.eclipse.gmt.modisco.java.Package;
import org.eclipse.gmt.modisco.java.emf.JavaFactory;
import org.junit.Assert;
import org.junit.Test;

public class ParsingUtilsTest {

    @Test
    public void testFindOverridenMethodInDirectSuperClass() {
        Model model = JavaFactory.eINSTANCE.createModel();
        model.setName("Model");

        Package aPackage = JavaFactory.eINSTANCE.createPackage();
        aPackage.setName("package");
        aPackage.setModel(model);

        ClassDeclaration clazz = JavaFactory.eINSTANCE.createClassDeclaration();
        ClassDeclaration superClazz = JavaFactory.eINSTANCE.createClassDeclaration();

        clazz.setPackage(aPackage);
        superClazz.setPackage(aPackage);
        clazz.setName("Clazz");
        superClazz.setName("SuperClazz");

        Assert.assertEquals("package.Clazz", ModelUtils.getQualifiedName(clazz));
        Assert.assertEquals("package.SuperClazz", ModelUtils.getQualifiedName(superClazz));

        MethodDeclaration method = JavaFactory.eINSTANCE.createMethodDeclaration();
        MethodDeclaration superMethod = JavaFactory.eINSTANCE.createMethodDeclaration();

        method.setName("method");
        superMethod.setName("method");

        clazz.getBodyDeclarations().add(method);
        superClazz.getBodyDeclarations().add(superMethod);

        TypeAccess typeAccess = JavaFactory.eINSTANCE.createTypeAccess();
        typeAccess.setType(superClazz);
        clazz.setSuperClass(typeAccess);

        Assert.assertEquals("package.SuperClazz$method", ParsingUtils.findOverridenMethodInSuperClass(clazz, "method"));
    }

    @Test
    public void testFindOverridenMethodInSuperClasses() {
        Model model = JavaFactory.eINSTANCE.createModel();
        model.setName("Model");

        Package aPackage = JavaFactory.eINSTANCE.createPackage();
        aPackage.setName("package");
        aPackage.setModel(model);

        ClassDeclaration clazz = JavaFactory.eINSTANCE.createClassDeclaration();
        ClassDeclaration superClazz = JavaFactory.eINSTANCE.createClassDeclaration();
        ClassDeclaration superSuperClazz = JavaFactory.eINSTANCE.createClassDeclaration();

        clazz.setPackage(aPackage);
        superClazz.setPackage(aPackage);
        superSuperClazz.setPackage(aPackage);

        clazz.setName("Clazz");
        superClazz.setName("SuperClazz");
        superSuperClazz.setName("SuperSuperClazz");

        Assert.assertEquals("package.Clazz", ModelUtils.getQualifiedName(clazz));
        Assert.assertEquals("package.SuperClazz", ModelUtils.getQualifiedName(superClazz));
        Assert.assertEquals("package.SuperSuperClazz", ModelUtils.getQualifiedName(superSuperClazz));

        MethodDeclaration method = JavaFactory.eINSTANCE.createMethodDeclaration();
        MethodDeclaration superMethod = JavaFactory.eINSTANCE.createMethodDeclaration();

        method.setName("method");
        superMethod.setName("method");

        clazz.getBodyDeclarations().add(method);
        superSuperClazz.getBodyDeclarations().add(superMethod);

        TypeAccess typeAccess = JavaFactory.eINSTANCE.createTypeAccess();
        typeAccess.setType(superClazz);
        TypeAccess superTypeAccess = JavaFactory.eINSTANCE.createTypeAccess();
        typeAccess.setType(superSuperClazz);

        clazz.setSuperClass(typeAccess);
        superClazz.setSuperClass(superTypeAccess);

        Assert.assertEquals("package.SuperSuperClazz$method", ParsingUtils.findOverridenMethodInSuperClass(clazz, "method"));
    }

    @Test
    public void testNotFindOverridenMethodInSuperClasses() {
        Model model = JavaFactory.eINSTANCE.createModel();
        model.setName("Model");

        Package aPackage = JavaFactory.eINSTANCE.createPackage();
        aPackage.setName("package");
        aPackage.setModel(model);

        ClassDeclaration clazz = JavaFactory.eINSTANCE.createClassDeclaration();
        ClassDeclaration superClazz = JavaFactory.eINSTANCE.createClassDeclaration();
        ClassDeclaration superSuperClazz = JavaFactory.eINSTANCE.createClassDeclaration();

        clazz.setPackage(aPackage);
        superClazz.setPackage(aPackage);
        superSuperClazz.setPackage(aPackage);

        clazz.setName("Clazz");
        superClazz.setName("SuperClazz");
        superSuperClazz.setName("SuperSuperClazz");

        Assert.assertEquals("package.Clazz", ModelUtils.getQualifiedName(clazz));
        Assert.assertEquals("package.SuperClazz", ModelUtils.getQualifiedName(superClazz));
        Assert.assertEquals("package.SuperSuperClazz", ModelUtils.getQualifiedName(superSuperClazz));

        MethodDeclaration method = JavaFactory.eINSTANCE.createMethodDeclaration();

        method.setName("method");

        clazz.getBodyDeclarations().add(method);

        TypeAccess typeAccess = JavaFactory.eINSTANCE.createTypeAccess();
        typeAccess.setType(superClazz);
        TypeAccess superTypeAccess = JavaFactory.eINSTANCE.createTypeAccess();
        typeAccess.setType(superSuperClazz);

        clazz.setSuperClass(typeAccess);
        superClazz.setSuperClass(superTypeAccess);

        Assert.assertEquals(null, ParsingUtils.findOverridenMethodInSuperClass(clazz, "method"));
    }

    @Test
    public void testGetQualifiedNameSimpleMethod() {
        String s = "package mainPackage; public class Clazz { public static void main(String[] args) { System.out.println(\"Hello, world !\"); }}";
        CompilationUnit compilationUnit = JavaParser.parse(s);

        com.github.javaparser.ast.body.MethodDeclaration methodDeclaration = compilationUnit.getChildNodesByType(com.github.javaparser.ast.body.MethodDeclaration.class).get(0);
        Assert.assertEquals("mainPackage.Clazz$main", ParsingUtils.getQualifiedName(methodDeclaration));
    }

    @Test
    public void testGetQualifiedNameSimpleClass() {
        String s = "package mainPackage; public class Clazz { public static void main(String[] args) { System.out.println(\"Hello, world !\"); }}";
        CompilationUnit compilationUnit = JavaParser.parse(s);

        ClassOrInterfaceDeclaration classOrInterfaceDeclaration = compilationUnit.getChildNodesByType(ClassOrInterfaceDeclaration.class).get(0);
        Assert.assertEquals("mainPackage.Clazz", ParsingUtils.getQualifiedName(classOrInterfaceDeclaration));
    }

    @Test
    public void testGetQualifiedNameInnerClass() {
        String s = "package mainPackage; public class Clazz { " +
                "class InnerClass {" +
                "int attribute;" +
                "}" +
                "public static void main(String[] args) { System.out.println(\"Hello, world !\"); }}";

        CompilationUnit compilationUnit = JavaParser.parse(s);

        ClassOrInterfaceDeclaration classOrInterfaceDeclaration = compilationUnit.getChildNodesByType(ClassOrInterfaceDeclaration.class).stream().filter(classOrInterfaceDeclaration1 -> "InnerClass".equals(classOrInterfaceDeclaration1.getNameAsString())).findFirst().get();
        Assert.assertEquals("mainPackage.Clazz$InnerClass", ParsingUtils.getQualifiedName(classOrInterfaceDeclaration));
    }

    @Test
    public void testGetQualifiedNameInnerMethod() {
        String s = "package mainPackage; public class Clazz { " +
                "class InnerClass {" +
                "int attribute;" +
                "public static void main(String[] args) { System.out.println(\"Hello, world !\"); }" +
                "}" +
                "}";

        CompilationUnit compilationUnit = JavaParser.parse(s);

        com.github.javaparser.ast.body.MethodDeclaration innerMethod = compilationUnit.getChildNodesByType(com.github.javaparser.ast.body.MethodDeclaration.class).stream().filter(m -> "main".equals(m.getNameAsString())).findFirst().get();

        Assert.assertEquals("mainPackage.Clazz$InnerClass$main", ParsingUtils.getQualifiedName(innerMethod));
    }
}

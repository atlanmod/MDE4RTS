package com.tblf.parsingbehaviors;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.tblf.utils.ModelUtils;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.gmt.modisco.java.BodyDeclaration;
import org.eclipse.gmt.modisco.java.ClassDeclaration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ParsingUtils {

    static Map<String, ClassDeclaration> qualifiedNameEObjectMap;

    /**
     * Iterates over the parents nodes of a class to get the qualified name
     *
     * @param methodDeclaration a {@link MethodDeclaration}
     * @return a Qualified name as a {@link String}
     */
    public static String getQualifiedName(MethodDeclaration methodDeclaration) {
        String QN = "";
        if (methodDeclaration.getParentNode().isPresent() && methodDeclaration.getParentNode().get() instanceof ClassOrInterfaceDeclaration) {
            QN = getQualifiedName((ClassOrInterfaceDeclaration) methodDeclaration.getParentNode().get()).concat("$");
        }

        return QN + methodDeclaration.getNameAsString();
    }

    public static String getQualifiedName(ClassOrInterfaceDeclaration classOrInterfaceDeclaration) {
        String QN = "";

        if (classOrInterfaceDeclaration.getParentNode().isPresent() && classOrInterfaceDeclaration.getParentNode().get() instanceof ClassOrInterfaceDeclaration) {
            QN = getQualifiedName((ClassOrInterfaceDeclaration) classOrInterfaceDeclaration.getParentNode().get()).concat("$");
        } else {
            CompilationUnit compilationUnit = classOrInterfaceDeclaration.getAncestorOfType(CompilationUnit.class).orElse(new CompilationUnit());
            if (compilationUnit.getPackageDeclaration().isPresent()) {
                QN = QN.concat(compilationUnit.getPackageDeclaration().get().getName() + ".");
            }
        }



        return QN+classOrInterfaceDeclaration.getName();
    }


    /**
     * Find the method that is overriden in the super classes
     * @param methodDeclaration the {@link MethodDeclaration}
     * @param folder a {@link File} folder containing the code (and the model)
     */
    public static String getMethodOverriden(MethodDeclaration methodDeclaration, File folder) {

        if (qualifiedNameEObjectMap == null) {
            //Build the class map if it does not exist yet
            qualifiedNameEObjectMap = new HashMap<>();

            try {
                Files.walk(folder.toPath(), 1).filter(path -> path.toString().endsWith("_java.xmi")).forEach(path -> {
                    try {
                        Resource staticModel = ModelUtils.loadModel(path.toFile());
                        staticModel.getAllContents().forEachRemaining(eObject -> {
                            if (eObject instanceof ClassDeclaration) {
                                qualifiedNameEObjectMap.put(ModelUtils.getQualifiedName(eObject), (ClassDeclaration) eObject);
                            }
                        });
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Get the class in the model in order to find the overriden classes
        ClassOrInterfaceDeclaration classOrInterfaceDeclaration = (ClassOrInterfaceDeclaration) methodDeclaration.getParentNode().get();
        ClassDeclaration classInTheModel = qualifiedNameEObjectMap.get(getQualifiedName(classOrInterfaceDeclaration));

        return findOverridenMethodInSuperClass(classInTheModel, methodDeclaration.getNameAsString());
    }

    /**
     * Recursively look for the qualified name of the method that is overriden in the superClasses
     * @param classDeclaration
     * @param methodName the name of the method
     * @return the Qualified name of the method, or null if none is found
     */
    public static String findOverridenMethodInSuperClass(ClassDeclaration classDeclaration, String methodName) {
        if (classDeclaration.getSuperClass() != null && classDeclaration.getSuperClass().getType() instanceof ClassDeclaration) {
            ClassDeclaration superClass = (ClassDeclaration) classDeclaration.getSuperClass().getType();
            Optional<BodyDeclaration> methodOverriden = superClass.getBodyDeclarations()
                        .stream()
                        .filter(bodyDeclaration -> bodyDeclaration instanceof org.eclipse.gmt.modisco.java.MethodDeclaration
                            && methodName.equals(bodyDeclaration.getName())) //TODO Add filter on method parameters
                        .findFirst();

            return methodOverriden.map(ModelUtils::getQualifiedName)
                    .orElseGet(() -> findOverridenMethodInSuperClass((ClassDeclaration) classDeclaration.getSuperClass().getType(), methodName));

        } else return null;
    }

    /**
     * Check whether a MethodDeclaration is a test or not
     * @param methodDeclaration a {@link MethodDeclaration}
     * @return true if is a test, false if not
     */
    public static boolean isTest(MethodDeclaration methodDeclaration) {
        return methodDeclaration.getAnnotations().stream().anyMatch(annotationExpr -> "Test".equals(annotationExpr.getNameAsString()));
    }


}

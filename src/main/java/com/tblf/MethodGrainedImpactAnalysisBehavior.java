package com.tblf;

import com.tblf.parsing.parsingBehaviors.ParsingBehavior;
import com.tblf.utils.ModelUtils;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceImpl;
import org.eclipse.gmt.modisco.java.AbstractMethodDeclaration;
import org.eclipse.gmt.modisco.java.AbstractTypeDeclaration;
import org.eclipse.gmt.modisco.java.MethodDeclaration;
import org.eclipse.gmt.modisco.java.TypeDeclaration;
import org.omg.smm.*;

import java.io.File;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;

public class MethodGrainedImpactAnalysisBehavior extends ParsingBehavior {
    private Map<String, AbstractTypeDeclaration> stringTypeDeclarationMap;
    private Map<AbstractMethodDeclaration, Measurement> methodDeclarationMeasurementMap;

    private static final SmmFactory FACTORY = SmmFactory.eINSTANCE;

    private ObservedMeasure observedMeasure;
    private Observation observation;

    private Measurement testRunning;


    public MethodGrainedImpactAnalysisBehavior(ResourceSet model) {
        super(model);

        model.getResources().removeIf(resource -> !resource.getURI().toString().contains("_java.xmi"));

        Resource jModel = model.getResources().stream().filter(resource -> resource.getURI().toString().contains("_java.xmi")).findFirst().get();

        stringTypeDeclarationMap = new HashMap<>();
        methodDeclarationMeasurementMap = new HashMap<>();

        //Build an index of the methods
        jModel.getAllContents().forEachRemaining(eObject -> {
            if (eObject instanceof AbstractTypeDeclaration) {
                String qn = ModelUtils.getQualifiedName(eObject);
                stringTypeDeclarationMap.put(qn, (AbstractTypeDeclaration) eObject);
            }
        });

        SmmPackage.eINSTANCE.eClass();

        MeasureLibrary measureLibrary = FACTORY.createMeasureLibrary();

        Measure callGraphMeasure = FACTORY.createBinaryMeasure();
        callGraphMeasure.setName("impacts");

        measureLibrary.getMeasureElements().add(callGraphMeasure);

        SmmModel smmModel = FACTORY.createSmmModel();
        smmModel.getLibraries().add(measureLibrary);

        observation = FACTORY.createObservation();
        smmModel.getObservations().add(observation);

        observedMeasure = FACTORY.createObservedMeasure();
        observedMeasure.setMeasure(callGraphMeasure);

        try {
            Resource resource = new XMIResourceImpl();

            resource.setURI(URI.createURI(new File(new File(jModel.getURI().toFileString()).getParentFile(), "smmGraph.xmi").toURI().toURL().toString()));
            resource.getContents().add(smmModel);
            model.getResources().add(resource);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void manage(String trace) {
        boolean isTest = false;
        
        if (trace.startsWith(":")) { //New test, clear the stack
            isTest = true;
            trace = trace.replace(":", "");
        }

        boolean isExitPoint = false;

        if (trace.startsWith(";")) { //end of method, remove the method
            return;
        }

        trace = trace.replace("\n", "");

        String[] splittedTrace = trace.split("\\$");
        String className, methodName;

        if (splittedTrace.length > 2) {
            // Consider internal classes
            className = trace.substring(0, trace.lastIndexOf("$"));
            methodName = splittedTrace[splittedTrace.length-1];
        } else {
            className = trace.split("\\$")[0];
            methodName = trace.split("\\$")[1];
        }

        if ("<init>".equals(methodName)) methodName = className;


        String qualifiedName = className+"$"+methodName;

        AbstractTypeDeclaration typeDeclaration = stringTypeDeclarationMap.get(className);
        if (typeDeclaration != null) {
            AbstractMethodDeclaration methodDeclaration = getMethodDeclarationFromTypeOrSuperTypes(typeDeclaration, methodName);

            if (isTest) {
                testRunning = createMeasurement(methodDeclaration, qualifiedName);
            } else {
                createImpactRelationship(methodDeclaration, qualifiedName, testRunning);
            }
        }
    }

    private void createImpactRelationship(AbstractMethodDeclaration methodDeclaration, String methodName, Measurement testRunning) {
        Measurement measurement = methodDeclarationMeasurementMap.get(methodDeclaration);
        if (measurement == null) {
            measurement = createMeasurement(methodDeclaration, methodName);
            methodDeclarationMeasurementMap.put(methodDeclaration, measurement);
        }

        if (testRunning != null && measurement.getMeasurementRelationships().stream().noneMatch(measurementRelationship -> measurementRelationship.getFrom() == testRunning)) {
            BaseNMeasurementRelationship baseNMeasurementRelationship = FACTORY.createBaseNMeasurementRelationship();
            baseNMeasurementRelationship.setName("follows");
            baseNMeasurementRelationship.setTo(measurement);
            baseNMeasurementRelationship.setFrom(testRunning);
            measurement.getMeasurementRelationships().add(baseNMeasurementRelationship);
        }
    }

    private Measurement createMeasurement(AbstractMethodDeclaration methodDeclaration, String methodName) {
        BinaryMeasurement binaryMeasurement = FACTORY.createBinaryMeasurement();
        binaryMeasurement.setName(methodName);
        binaryMeasurement.setMeasurand(methodDeclaration);
        observedMeasure.getMeasurements().add(binaryMeasurement);
        return binaryMeasurement;
    }

    private AbstractMethodDeclaration getMethodDeclarationFromTypeOrSuperTypes(AbstractTypeDeclaration typeDeclaration, String methodDeclaration) {
        return (MethodDeclaration) typeDeclaration.getBodyDeclarations().stream()
            .filter(bodyDeclaration -> bodyDeclaration instanceof MethodDeclaration)
            .filter(bodyDeclaration -> bodyDeclaration.getName().equals(methodDeclaration))
            .findFirst()
            .orElseGet(() -> {
                if (typeDeclaration.getAbstractTypeDeclaration() instanceof TypeDeclaration)
                    return getMethodDeclarationFromTypeOrSuperTypes(typeDeclaration.getAbstractTypeDeclaration(), methodDeclaration);
                else
                    return null;
            });
    }

    public void close() {
        try {
            observation.getObservedMeasures().add(observedMeasure);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}

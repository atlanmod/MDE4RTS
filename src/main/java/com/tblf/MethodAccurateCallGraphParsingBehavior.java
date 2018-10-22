package com.tblf;

import com.tblf.parsing.indexer.HawkQuery;
import com.tblf.parsing.parsingBehaviors.ParsingBehavior;
import com.tblf.utils.ModelUtils;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceImpl;
import org.eclipse.gmt.modisco.java.*;
import org.omg.smm.*;

import java.io.File;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

public class MethodAccurateCallGraphParsingBehavior extends ParsingBehavior {
    private Map<String, AbstractTypeDeclaration> stringTypeDeclarationMap;

    private static final SmmFactory FACTORY = SmmFactory.eINSTANCE;

    private Observation observation;

    private Measure callGraphMeasure;

    private Stack<BinaryMeasurement> methodStack;

    private HawkQuery hawkQuery;

    public MethodAccurateCallGraphParsingBehavior(ResourceSet model) {
        super(model);
        File file = new File(model.getResources().get(0).getURI().toString());
        hawkQuery = new HawkQuery(file.getParentFile());
        model.getResources().removeIf(resource -> !resource.getURI().toString().contains("_java.xmi"));

        Resource jModel = model.getResources().stream().filter(resource -> resource.getURI().toString().contains("_java.xmi")).findFirst().get();

        stringTypeDeclarationMap = new HashMap<>();
        methodStack = new Stack<>();

        //Build an index of the methods
        jModel.getAllContents().forEachRemaining(eObject -> {
            if (eObject instanceof AbstractTypeDeclaration) {
                String qn = ModelUtils.getQualifiedName(eObject);
                stringTypeDeclarationMap.put(qn, (AbstractTypeDeclaration) eObject);
            }
        });

        SmmPackage.eINSTANCE.eClass();

        MeasureLibrary measureLibrary = FACTORY.createMeasureLibrary();

        UnitOfMeasure uj = FACTORY.createUnitOfMeasure();
        uj.setName("follows");
        uj.setDescription("Gather the call graph of a program execution");

        callGraphMeasure = FACTORY.createCollectiveMeasure();

        measureLibrary.getMeasureElements().add(callGraphMeasure);

        SmmModel smmModel = FACTORY.createSmmModel();
        smmModel.getLibraries().add(measureLibrary);

        observation = FACTORY.createObservation();

        smmModel.getObservations().add(observation);

        try {
            Resource resource = new XMIResourceImpl();

            resource.setURI(URI.createURI(new File(new File(jModel.getURI().toFileString()).getParentFile(), "smmGraph.xmi").toURI().toURL().toString()));
            resource.getContents().add(smmModel);
            model.getResources().add(resource);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

    }


    public void manage(String trace) {
        if (trace.startsWith(":")) { //New test, clear the stack
            methodStack.clear();
            trace = trace.replace(":", "");
        }

        boolean isExitPoint = false;

        if (trace.startsWith(";")) { //end of method, remove the method
            isExitPoint = true;
            trace = trace.replace(";", "");
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

        AbstractTypeDeclaration typeDeclaration = stringTypeDeclarationMap.get(className);
        AbstractMethodDeclaration methodDeclaration = getMethodDeclarationFromTypeOrSuperTypes(typeDeclaration, methodName);

        if (isExitPoint) {
            methodStack.pop();
        } else
            startMethodExecution(methodDeclaration, methodName);

    }

    /**
     * \\TODO Consider parameters when querying methods.
     * @param methodQN the Qualified name of a method, as a {@link String} e.g.: com.tblf.Clazz$methodName
     */
    private void startMethodExecution(AbstractMethodDeclaration methodDeclaration, String methodQN) {
        BinaryMeasurement measurement = createRootMeasurement(methodDeclaration);
        measurement.setName(methodQN);

        ObservedMeasure observedMeasure = FACTORY.createObservedMeasure();
        observedMeasure.getMeasurements().add(measurement);
        observedMeasure.setMeasure(callGraphMeasure);

        observation.getObservedMeasures().add(observedMeasure);
        methodStack.push(measurement);
    }

    /**
     * Create the {@link CollectiveMeasurement} containings the child {@link DimensionalMeasurement}.
     * Also set up the call relationship between methods
     *
     * @param measurand an {@link EObject}, usually a {@link org.eclipse.gmt.modisco.java.MethodDeclaration}
     * @return a {@link CollectiveMeasurement}
     */
    private BinaryMeasurement createRootMeasurement(EObject measurand) {
        BinaryMeasurement measurement = FACTORY.createBinaryMeasurement();
        measurement.setMeasurand(measurand);

        if (!methodStack.empty()) {
            BinaryMeasurement previous = methodStack.firstElement();

            //definition of the follows relationship
            BaseNMeasurementRelationship baseNMeasurementRelationship = FACTORY.createBaseNMeasurementRelationship();
            baseNMeasurementRelationship.setName("follows");
            baseNMeasurementRelationship.setTo(measurement);
            baseNMeasurementRelationship.setFrom(previous);

            previous.getOutbound().add(baseNMeasurementRelationship);
            measurement.getInbound().add(baseNMeasurementRelationship);
            measurement.getMeasurementRelationships().add(baseNMeasurementRelationship);

        }

        return measurement;
    }

    /**
     * From a {@link TypeDeclaration}, check if the methodDeclaration with the given QN as {@link String} exist, and if yes, returns it.
     * If not, check in the parent classes until finding the right class
     * @param typeDeclaration a {@link TypeDeclaration}
     * @return the corresponding {@link MethodDeclaration}
     */
    private MethodDeclaration getMethodDeclarationFromTypeOrSuperTypes(AbstractTypeDeclaration typeDeclaration, String methodDeclaration) {
        return (MethodDeclaration) typeDeclaration.getBodyDeclarations().stream()
                .filter(bodyDeclaration -> bodyDeclaration instanceof MethodDeclaration)
                .filter(bodyDeclaration -> bodyDeclaration.getName().equals(methodDeclaration))
                .findFirst()
                .orElseGet(() -> {
                    if (typeDeclaration.getAbstractTypeDeclaration() instanceof TypeDeclaration)
                        return getMethodDeclarationFromTypeOrSuperTypes((TypeDeclaration) typeDeclaration.getAbstractTypeDeclaration(), methodDeclaration);
                    else
                        return null;
                });
    }

    public void close() {
        try {
            hawkQuery.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

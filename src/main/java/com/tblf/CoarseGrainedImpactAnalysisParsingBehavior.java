package com.tblf;

import com.tblf.parsing.parsingBehaviors.ParsingBehavior;
import com.tblf.utils.ModelUtils;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceImpl;
import org.eclipse.gmt.modisco.java.AbstractMethodDeclaration;
import org.eclipse.gmt.modisco.java.ConstructorDeclaration;
import org.omg.smm.*;

import java.io.File;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

public class CoarseGrainedImpactAnalysisParsingBehavior extends ParsingBehavior {
    private Map<String, AbstractMethodDeclaration> stringMethodDeclarationMap;
    private static final SmmFactory FACTORY = SmmFactory.eINSTANCE;

    private Observation observation;

    private Measure callGraphMeasure;

    private Stack<BinaryMeasurement> methodStack;

    public CoarseGrainedImpactAnalysisParsingBehavior(ResourceSet model) {
        super(model);

        model.getResources().removeIf(resource -> !resource.getURI().toString().contains("_java.xmi"));

        Resource jModel = model.getResources().stream().filter(resource -> resource.getURI().toString().contains("_java.xmi")).findFirst().get();

        stringMethodDeclarationMap = new HashMap<>();
        methodStack = new Stack<>();

        //Build an index of the methods
        jModel.getAllContents().forEachRemaining(eObject -> {
            if (eObject instanceof AbstractMethodDeclaration) {
                if (eObject instanceof ConstructorDeclaration) {
                    ConstructorDeclaration constructorDeclaration = (ConstructorDeclaration) eObject;
                    constructorDeclaration.setName("<init>");
                }
                String qn = ModelUtils.getQualifiedName(eObject);
                stringMethodDeclarationMap.put(qn, (AbstractMethodDeclaration) eObject); //faster to add "\n" here than cutting the traces
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

        if (trace.startsWith(":")) {
            methodStack.clear();
            trace = trace.replace(":", "");
        }

        if (trace.startsWith(";")) { //end of method
            if (!methodStack.isEmpty()) {
                methodStack.pop();
            }
        } else {
            //start of method
            startMethodExecution(trace);
        }
    }

    private void startMethodExecution(String methodQN) {
        methodQN = methodQN.replace("\n", "");
        AbstractMethodDeclaration methodDeclaration = stringMethodDeclarationMap.get(methodQN);

        if (methodDeclaration == null)
            System.out.println("Couldn't find "+methodQN+" in the model");

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
            BinaryMeasurement previous = methodStack.peek();

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
}

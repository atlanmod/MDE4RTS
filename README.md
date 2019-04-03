# MDE4RTS

#  Installation

MDE4RTS will download main dependencies using maven. However several dependencies have to be installed by hand before-hand, including [Mondo-hawk](https://github.com/mondo-project/mondo-hawk), the [DynamicAnalyser](https://github.com/atlanmod/DynamicAnalyser), and [MoDisco](https://www.eclipse.org/MoDisco/).

A simple headless Eclipse plugin is available in the jar located in src/main/resources. It takes as a parameter the absolute URI of the project under analysis. 

# Approach
## Building the model

This first step can be executed at any time (i.e. offline). The project on which the model is going to be built needs to be a Maven project, containing Java Source Code.

A MoDisco model is necessary to build the **Impact Analysis Model**. It can be built by hand before building the Impact Analysis Model. MoDisco being an Eclipse Plugin, it could not be embedded in this project.

We propose two different ways of running the Impact Analysis in our approach. Both need the code to be executed before. However, the first one is model-based, whereas the second one only rely on the execution traces.

### Model-Based Impact Analysis

The test cases are going to be executed after instrumenting the bytecode, and execution traces will to be gathered. 
Those traces are then analysed, in order to build a model, persisted in the corresponding .xmi file.

An example is given below:

![alt text](pics/mbia.png?raw=true "mbia")

The static model is generated out of the source code using modisco. The code is then instrumented and executed. It produces an execution trace, that is then analyzed. The analysis finally inserts, in the static model, references, used latter to perform the impact analysis.

### Trace-Based Impact Analysis

This approach is way faster, however the impact analysis model is not persisted using EMF, which reduces the reusability.
Here, the static model is generated with MoDisco, and the code is instrumented before being executed. 

An example is given below:
![alt text](pics/tia.png?raw=true "tbia")

The execution traces are written in a topic-based queue, that is available for performing the impact analysis latter. Each topic corresponds to a method of the system-under-test. Hence, if this method is modified during code changes, all the messages in this topic will correspond to the section of the code impacted.

## Performing the Regression Test Selection

An Impact Analysis Model is necessary to perform the RTS. The RTS can be either performed between the last git commit done on the repository and the current state of the code, or from a previous commit and the current state of the code.

Running the RTS will print in the console the qualified names of the test methods impacted.
# Execution

```
usage: java -jar mde4rts.jar
 -help                   print this message
 -model                  build the static model using modisco
 -iamodel                build the impact analysis model without
                         performing a RTS
 -project <projectURI>   project on which the operations will be performed
 -rts <commitID>         perform a regression test selection between the
                         current revision and the specified commit.
                         Specify no commit if you want to apply RTS
                         between master and HEAD
```

Exemple:

`java -jar -iamodel -project ~/workspace/a_maven_project/`
- Would build the impact analysis model of the project `a_maven_project`.

`java -jar -rts -project ~/workspace/a_maven_project/`
- Would run the RTS on the project `a_maven_project`, comparing the current state of the program with the last commit.

`java -jar -rts 5h6d3v22 -project ~/workspace/a_maven_project/`
- Would run the RTS on the project `a_maven_project`, comparing the current state of the program with the commit  `5h6d3v22`.
 

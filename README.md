# MDE4RTS

#  Installation

MDE4RTS will download main dependencies using maven. However several dependencies have to be installed by hand before-hand, including [Mondo-hawk](https://github.com/mondo-project/mondo-hawk), and [MoDisco](https://www.eclipse.org/MoDisco/).

A simple headless Eclipse plugin is available in the jar located in src/main/resources. It takes as a parameter the absolute URI of the project under analysis. 

# Approach
## Building the model

This first step can be executed at any time (i.e. offline). The project on which the model is going to be built needs to be a Maven project, containing Java Source Code.

The test cases are going to be executed after instrumenting the bytecode, and execution traces will to be gathered. 
Those traces are then analysed, in order to build a model, persisted in the corresponding .xmi file.

A MoDisco model is necessary to build the **Impact Analysis Model**. It can be built by hand before building the Impact Analysis Model. MoDisco being an Eclipse Plugin, it could not be embedded in this project.

## Performing the Regression Test Selection

An Impact Analysis Model is necessary to perform the RTS. The RTS can be either performed between the last git commit done on the repository and the current state of the code, or from a previous commit and the current state of the code.

Running the RTS will print in the console the qualified names of the test methods impacted.
# Execution

```
usage: java -jar mde4rts.jar
 -help                   print this message
 -model                  build the impact analysis model without
                         performing a RTS
 -project <projectURI>   project on which the operations will be performed
 -rts <commitID>         perform a regression test selection between the
                         current revision and the specified commit.
                         Specify no commit if you want to apply RTS
                         between master and HEAD
```

Exemple:

`java -jar -model -project ~/workspace/a_maven_project/`
- Would build the impact analysis model of the project `a_maven_project`.

`java -jar -rts -project ~/workspace/a_maven_project/`
- Would run the RTS on the project `a_maven_project`, comparing the current state of the program with the last commit.

`java -jar -rts 5h6d3v22 -project ~/workspace/a_maven_project/`
- Would run the RTS on the project `a_maven_project`, comparing the current state of the program with the commit  `5h6d3v22`.
 

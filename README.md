# derg
DERG (.dex element relation graph) is a graph representation of code.
It is general representation which can be used in Android app reverse-engineering, for example:

- Getting the class hierarchy (inherit, implements, etc.) of an Android app;
- Extracting the APIs used in each method;
- Extracting the call-return relation (a simplified call-graph);
- Extracting the define-use relation between methods and fields.

# Prerequisite
- Java 7
- Maven

# Installation
1. Clone this repo.
```
git clone https://github.com/ylimit/derg.git
```
2. Compile with Maven
```
cd derg
mvn package
```
Then find the compiled jar file in `target` directory.

# Usage
Please type `java -jar DERG.jar -h` to see detailed usage information.
Typical usage:
```
java -jar DERG-1.0.jar -i <your_app.apk> -o <output_dir> -f soot -b graph_export -sdk ~/Android/Sdk/platforms/android-22/android.jar
```
This command will generate the DERG representation of an app.

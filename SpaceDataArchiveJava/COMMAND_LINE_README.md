# Space Data Archive System - Command Line Guide

This guide explains how to work with the Space Data Archive System Java application purely from the command line, without relying on any IDE.

## Prerequisites

- Maven 3.8 or newer (which will handle the Java installation)
- Bash shell or compatible terminal

### Installing Maven on macOS

The simplest way to install Maven on macOS is through Homebrew:

```bash
brew install maven
```

Maven will automatically locate and use an appropriate Java Development Kit, so you don't need to install Java separately or configure environment variables.

## Getting Started

1. First, clean up any IDE-specific configuration files (if you've previously used an IDE):

```bash
./cleanup-ide-files.sh
```

2. Build and run the application:

```bash
./build-and-run.sh
```

This script handles cleaning, compiling, packaging, and launching the application using Maven, which takes care of all the Java-related configurations automatically.

## Manual Steps

If you prefer to run the steps manually:

### Cleaning the Project

```bash
mvn clean
```

### Compiling

```bash
mvn compile
```

### Packaging

```bash
mvn package
```

### Running the Application

```bash
mvn exec:java
```

Maven's exec plugin will handle all the necessary JVM arguments and classpath settings.

## Troubleshooting

### Maven Not Found

If you get "Maven not found" errors, install Maven:

```bash
brew install maven
```

### Java-Related Errors

If Maven reports Java-related errors:

1. Let Maven handle the Java installation:
   ```bash
   brew install openjdk@17
   ```
   
2. Follow Homebrew's instructions to link the Java installation properly.

3. If needed, set JAVA_HOME explicitly before running Maven:
   ```bash
   export JAVA_HOME=$(/usr/libexec/java_home)
   ./build-and-run.sh
   ```

### Execution Issues

If the application doesn't start correctly, try running the build steps separately:

```bash
mvn clean compile package
mvn exec:java
```

## Customizing Build Options

The project uses standard Maven configuration in the `pom.xml` file. You can modify this file to adjust compiler settings, dependencies, or other build options as needed. 
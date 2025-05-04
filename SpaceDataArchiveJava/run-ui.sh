#!/bin/bash

# Ensure target/deps directory exists
mkdir -p target/deps

# Set up class path with all dependencies
CLASSPATH="target/classes"
for jar in target/deps/*.jar; do
  CLASSPATH="$CLASSPATH:$jar"
done

# Run the UI with JavaFX modules
/opt/homebrew/Cellar/openjdk/23.0.2/libexec/openjdk.jdk/Contents/Home/bin/java \
  -cp "$CLASSPATH" \
  --module-path target/deps \
  --add-modules=javafx.controls,javafx.fxml,javafx.graphics,javafx.web,javafx.media,javafx.base \
  -Djavafx.verbose=true \
  -Djava.library.path=target/deps \
  com.spacedataarchive.MainApp 
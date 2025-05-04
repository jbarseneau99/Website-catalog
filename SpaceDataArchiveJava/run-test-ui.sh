#!/bin/bash
# Test UI Launcher Script

echo "JavaFX Test UI"
echo "=============="

# Set the correct project directory
cd "$(dirname "$0")"

# Compile the test UI class if needed
mvn compile > /dev/null

# Prepare dependencies directory
mkdir -p target/deps

# Copy JavaFX libraries
find ~/.m2/repository/org/openjfx -name "*24.0.1*mac-aarch64.jar" -exec cp {} target/deps/ \;

# Copy other dependencies
mvn dependency:copy-dependencies -DincludeScope=runtime -DexcludeGroupIds=org.openjfx > /dev/null
find target/dependency -name "*.jar" ! -name "*javafx*" -exec cp {} target/deps/ \;

# Determine the correct Java path
if [ -x "/opt/homebrew/Cellar/openjdk/23.0.2/libexec/openjdk.jdk/Contents/Home/bin/java" ]; then
    JAVA_CMD="/opt/homebrew/Cellar/openjdk/23.0.2/libexec/openjdk.jdk/Contents/Home/bin/java"
else
    JAVA_CMD=$(which java)
fi

MODULE_PATH="$PWD/target/deps"
MODULES="javafx.controls,javafx.fxml,javafx.web,javafx.graphics"

echo "Starting Test UI..."
$JAVA_CMD \
  --module-path "$MODULE_PATH" \
  --add-modules "$MODULES" \
  -Djavafx.verbose=true \
  -Dprism.order=sw \
  -cp "$PWD/target/classes:$MODULE_PATH/*" \
  com.spacedataarchive.TestUI 
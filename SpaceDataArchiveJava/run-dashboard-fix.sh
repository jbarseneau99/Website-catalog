#!/bin/bash
# Operations Dashboard Launcher Script (Fixed version)

echo "Space Data Archive System - Operations Dashboard"
echo "==============================================="

# Set the correct project directory
cd "$(dirname "$0")"

# Use explicit Java path
JAVA_CMD="/opt/homebrew/Cellar/openjdk/23.0.2/libexec/openjdk.jdk/Contents/Home/bin/java"
if [ ! -x "$JAVA_CMD" ]; then
    echo "Error: Java not found at $JAVA_CMD"
    exit 1
fi

# Ensure our classes are compiled
echo "Compiling code..."
mvn compile

# Prepare dependencies directory
echo "Setting up dependencies..."
rm -rf target/deps
mkdir -p target/deps

# Copy JavaFX libraries (use specific version 24.0.1)
echo "Installing JavaFX libraries..."
find ~/.m2/repository/org/openjfx -name "*24.0.1*mac-aarch64.jar" -not -name "*sources*" -not -name "*javadoc*" -exec cp {} target/deps/ \;

# Copy other dependencies
mvn dependency:copy-dependencies -DincludeScope=runtime -DexcludeGroupIds=org.openjfx > /dev/null
find target/dependency -name "*.jar" ! -name "*javafx*" -exec cp {} target/deps/ \;

MODULE_PATH="$PWD/target/deps"
MODULES="javafx.controls,javafx.fxml,javafx.web,javafx.graphics"

echo "Starting Operations Dashboard..."
echo "Using Java: $JAVA_CMD"

$JAVA_CMD \
  --module-path "$MODULE_PATH" \
  --add-modules "$MODULES" \
  --add-opens java.base/java.lang=ALL-UNNAMED \
  --add-exports javafx.controls/com.sun.javafx.scene.control.behavior=ALL-UNNAMED \
  --add-exports javafx.controls/com.sun.javafx.scene.control=ALL-UNNAMED \
  --add-exports javafx.graphics/com.sun.javafx.scene=ALL-UNNAMED \
  -Djavafx.verbose=true \
  -DUSE_ATLANTA=true \
  -DATLANTA_THEME=PRIMER_LIGHT \
  -DWINDOW_WIDTH=1200 \
  -DWINDOW_HEIGHT=800 \
  -DWINDOW_CENTERED=true \
  -Dprism.order=sw \
  -cp "$PWD/target/classes:$MODULE_PATH/*" \
  com.spacedataarchive.LaunchOperationsDashboard 
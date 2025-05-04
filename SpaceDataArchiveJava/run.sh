#!/bin/bash
# Space Data Archive System - Main Run Script
# This script builds and runs the application with Atlanta theme

echo "Space Data Archive System - Running with Atlanta Theme"
echo "==================================================="

# Set the correct project directory
cd /Users/jbarseneau/Desktop/Website-catalog/SpaceDataArchiveJava

# Clean and build the application
echo "Building the application..."
mvn clean compile

# Prepare clean dependencies directory
echo "Setting up dependencies..."
rm -rf target/deps
mkdir -p target/deps

# Copy Apple Silicon compatible JavaFX libraries
echo "Installing JavaFX libraries..."
find ~/.m2/repository/org/openjfx -name "*24.0.1*mac-aarch64.jar" -exec cp {} target/deps/ \;

# Copy other dependencies
mvn dependency:copy-dependencies -DincludeScope=runtime -DexcludeGroupIds=org.openjfx
find target/dependency -name "*.jar" ! -name "*javafx*" -exec cp {} target/deps/ \;

# Determine the correct Java path
if [ -x "/opt/homebrew/Cellar/openjdk/23.0.2/libexec/openjdk.jdk/Contents/Home/bin/java" ]; then
    JAVA_CMD="/opt/homebrew/Cellar/openjdk/23.0.2/libexec/openjdk.jdk/Contents/Home/bin/java"
else
    JAVA_CMD=$(which java)
fi

MODULE_PATH="/Users/jbarseneau/Desktop/Website-catalog/SpaceDataArchiveJava/target/deps"
MODULES="javafx.controls,javafx.fxml,javafx.web,javafx.graphics"

# Set Atlanta theme parameters
# Options: PRIMER_LIGHT, PRIMER_DARK, NORD_LIGHT, NORD_DARK, CUPERTINO_LIGHT, CUPERTINO_DARK, DRACULA
ATLANTA_THEME="PRIMER_LIGHT"

echo "Starting application with Atlanta $ATLANTA_THEME theme..."
$JAVA_CMD \
  --module-path "$MODULE_PATH" \
  --add-modules "$MODULES" \
  --add-opens java.base/java.lang=ALL-UNNAMED \
  --add-exports javafx.controls/com.sun.javafx.scene.control.behavior=ALL-UNNAMED \
  --add-exports javafx.controls/com.sun.javafx.scene.control=ALL-UNNAMED \
  --add-exports javafx.graphics/com.sun.javafx.scene=ALL-UNNAMED \
  -Djavafx.verbose=true \
  -DUSE_ATLANTA=true \
  -DATLANTA_THEME=$ATLANTA_THEME \
  -DUSE_SYSTEM_THEME=true \
  -DENABLE_WEBPAGE_CLASSIFICATION=true \
  -DENABLE_URL_DIAGNOSTICS=true \
  -DWINDOW_WIDTH=1200 \
  -DWINDOW_HEIGHT=800 \
  -DWINDOW_CENTERED=true \
  -Dprism.order=sw \
  -cp "/Users/jbarseneau/Desktop/Website-catalog/SpaceDataArchiveJava/target/classes:$MODULE_PATH/*" \
  com.spacedataarchive.MainApp 
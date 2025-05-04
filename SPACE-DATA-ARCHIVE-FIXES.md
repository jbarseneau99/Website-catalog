# Space Data Archive System - Fixes for Apple Silicon (M-series) Macs

This document summarizes the issues encountered with the JavaFX application on Apple Silicon hardware and the fixes that were applied.

## Issues Encountered

1. **ClassNotFoundException for JavaFX classes**
   - The application couldn't find JavaFX modules like `javafx.scene.layout.BorderPane`

2. **Module conflicts**
   - Multiple JavaFX versions (11.0.2, 21, 22.0.1, 24.0.1) were causing conflicts

3. **Architecture incompatibility**
   - x86_64 libraries were being used on arm64 hardware

4. **Threading issues**
   - UI operations not running on the JavaFX application thread

5. **Resource loading error**
   - NullPointerException when loading an icon image due to an incorrect path

## Applied Fixes

### 1. JavaFX Dependencies

- Updated `pom.xml` to specify Apple Silicon compatible dependencies:
  ```xml
  <dependency>
      <groupId>org.openjfx</groupId>
      <artifactId>javafx-controls</artifactId>
      <version>24.0.1</version>
      <classifier>mac-aarch64</classifier>
  </dependency>
  ```

### 2. Dependency Cleanup

- Created scripts to clean up cached JavaFX libraries:
  ```bash
  rm -rf ~/.openjfx
  ```

### 3. Module Configuration

- Set up correct module path and modules:
  ```bash
  --module-path "target/deps"
  --add-modules "javafx.controls,javafx.fxml,javafx.web,javafx.graphics"
  ```

### 4. Thread Safety

- Fixed UI operations with `Platform.runLater()`:
  ```java
  Platform.runLater(() -> {
      // UI operations
  });
  ```

### 5. Icon Loading Fix

- Fixed icon loading by using the correct path and adding error handling:
  ```java
  try {
      Image appIcon = new Image(getClass().getResourceAsStream("/images/33fg_Logo.png"));
      if (appIcon != null && !appIcon.isError()) {
          this.primaryStage.getIcons().add(appIcon);
          logger.info("Application icon loaded successfully");
      } else {
          logger.warn("Could not load application icon - image error");
      }
  } catch (Exception e) {
      logger.warn("Could not load application icon: {}", e.getMessage());
      // Continue without the icon
  }
  ```

## Running the Application

A dedicated script `run-space-data-archive.sh` has been created to properly run the application:

1. Cleans up cached JavaFX libraries
2. Builds the application with Maven
3. Sets up a clean dependencies directory
4. Installs Apple Silicon compatible JavaFX libraries
5. Runs the application with the correct module configuration

To run the application:
```bash
./run-space-data-archive.sh
```

## Additional Notes

- The application now uses JavaFX 24.0.1 which has better Apple Silicon support
- All UI operations are wrapped in `Platform.runLater()` to ensure thread safety
- Error handling has been improved throughout the application
- Added `-Dprism.order=sw` to use software rendering if hardware acceleration causes issues
- Window size is now configurable via system properties:
  - `-DWINDOW_WIDTH=1200`: Sets the window width
  - `-DWINDOW_HEIGHT=800`: Sets the window height
  - `-DWINDOW_CENTERED=true`: Centers the window on the screen

## Project Cleanup and Organization

The project has been reorganized and cleaned up:

1. **Script Consolidation**
   - Simplified to just three main scripts:
     - `run.sh`: Main entry point in the root directory
     - `run-space-data-archive.sh`: Comprehensive script with all fixes
     - `SpaceDataArchiveJava/run.sh`: Internal project script

2. **Removed Redundant Files**
   - Deleted all experimental and temporary scripts
   - Removed Node.js related files that were not needed
   - Cleaned up backup files and duplicates

3. **Documentation Updates**
   - Updated README.md with clear running instructions
   - Consolidated troubleshooting information

## Troubleshooting

If issues persist:

1. Check Java version: Ensure you're using a native ARM64 Java build
   ```bash
   java -version
   ```

2. Verify Maven dependencies:
   ```bash
   mvn dependency:tree
   ```

3. Clean Maven cache if needed:
   ```bash
   mvn dependency:purge-local-repository
   ```

4. Check the logs for detailed error messages:
   ```bash
   cat SpaceDataArchiveJava/logs/application.log
   ``` 
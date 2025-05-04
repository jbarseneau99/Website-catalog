# Space Data Archive System - Java Client

This is a JavaFX desktop client application for the Space Data Archive System, providing a user-friendly interface for archiving and analyzing space-related data.

## Features

- Site map creation and web crawling for space-related websites
- URL validation and verification
- Logs and diagnostics for system monitoring
- Data extraction (coming in Phase 2)
- Analysis tools (coming in Phase 2)
- Search and access functionality (coming in Phase 3)
- Data catalog browsing (coming in Phase 3)
- AI Assistant powered by Anthropic Claude

## AI Assistant

The AI Assistant feature integrates Anthropic's Claude AI to provide intelligent assistance for space data analysis and research. Key capabilities include:

- Conversational interface with Claude AI models (Opus, Sonnet, Haiku)
- Persistent conversation history
- Model selection for different AI capabilities
- API key configuration
- Context-aware responses to space data queries

To use the AI Assistant:

1. Navigate to the AI Assistant tab in the application
2. Enter your Anthropic API key (obtain from https://console.anthropic.com)
3. Select a Claude model (Opus for best quality, Haiku for speed)
4. Start a new conversation or continue an existing one
5. Type your questions or commands related to space data analysis

## Requirements

- Java 17 or higher
- Maven 3.8 or higher

## Building the Application

To build the application, run the following command:

```
./run.sh
```

This will compile the code, package the application, and run it.

Alternatively, you can use Maven directly:

```
mvn clean package
mvn javafx:run
```

## Project Structure

- `src/main/java/com/spacedataarchive` - Main Java source code
  - `controller` - JavaFX controllers
  - `model` - Data models
  - `service` - Business logic and services
- `src/main/resources` - Application resources
  - `css` - CSS stylesheets
  - `fxml` - JavaFX FXML layouts

## Development Plan

### Phase 1: Project Setup & Infrastructure
- Basic UI with tabs
- Logging infrastructure
- Application framework

### Phase 2: Core Functionality
- Site Map Creation implementation
- URL Validation implementation
- Data Extraction implementation
- Analysis tools

### Phase 3: Advanced Features
- Search & Access functionality
- Data Catalog implementation
- API integration
- Reporting & Export features

### Phase 4: Polish & Optimization
- Performance optimizations
- UI/UX improvements
- Advanced error handling
- Packaging for distribution

## License

Copyright (c) 2023-2024 Space Data Archive Team

## Java Compatibility Notes

This project is configured to use Java 17, but is compatible with running on newer JDK versions (including JDK 23) with the proper configuration.

### IDE Configuration

If you're seeing the warning "The compiler compliance specified is 17 but a JRE 23 is used", use one of the following approaches:

#### Option 1: Run with JDK 17

Install JDK 17 on your system and configure your IDE to use it for this project.

#### Option 2: Configure IDE for Mixed JDK Environment

1. In Eclipse/VSCode/IntelliJ, set the project's JDK compliance level to 17
2. Set the compiler to use JDK 17 compatibility mode

### Running the Application

For the best compatibility when running on JDK 23 or other newer JDKs, use the included run script:

```bash
./run.sh
```

This script includes the necessary VM arguments to ensure JavaFX and other components work correctly with newer JDKs.

## Troubleshooting

### "Project configuration is not up-to-date with pom.xml"

If your IDE reports that the project configuration is not up-to-date with pom.xml:

1. Right-click on the project
2. Select "Maven" → "Update Project"
3. Check "Force Update of Snapshots/Releases"
4. Click "OK"

### "The build file has been changed and may need reload"

Run the following commands to refresh your Maven project:

```bash
mvn clean
mvn eclipse:clean
mvn eclipse:eclipse
```

Then refresh your project in your IDE. 
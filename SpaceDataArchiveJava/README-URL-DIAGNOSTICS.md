# URL Diagnostics System Integration

This document explains how the URL diagnostics system is integrated with the Space Data Archive Java application.

## Overview

The URL diagnostics system helps identify why URLs in a sitemap don't lead to assets by detecting issues such as:
- Dynamic content loading (AJAX, lazy loading)
- iframe-embedded content
- Non-standard asset formats
- Content requiring user interaction

The implementation consists of:
1. A JavaScript REST service that runs the diagnostics logic
2. A Java client that communicates with the service
3. A Java UI panel integrated into the main application

## Setup and Running

### Prerequisites
- Node.js and npm for the JavaScript service
- Java 11+ for the Java application
- Maven for building and running the Java application

### Running the System

1. Start the URL diagnostics service:
   ```
   ./run-diagnostic-service.sh
   ```

2. Run the Java application:
   ```
   ./run.sh
   ```

The `run.sh` script will automatically start the diagnostic service if it's not running.

## Architecture

### JavaScript Service (`src/url-diagnostic-service.js`)
- Provides a REST API for the Java application to call
- Endpoints:
  - `/analyze-url` - Analyze a single URL
  - `/analyze-urls` - Analyze multiple URLs
  - `/recent-urls` - Get recent URLs
  - `/load-urls` - Load URLs from a file
  - `/health` - Check service availability

### Java Client (`com.spacearchive.diagnostics.UrlDiagnosticClient`)
- Makes HTTP requests to the JavaScript service
- Parses JSON responses into Java objects
- Provides a clean Java API for the diagnostics functionality

### Java UI Panel (`com.spacearchive.ui.UrlDiagnosticsPanel`)
- Provides a user interface for the diagnostics functionality
- Allows users to:
  - Enter a URL to analyze
  - View diagnostic results
  - See recently analyzed URLs

## Data Model

The Java client uses two main data classes:

### `DiagnosticResult`
Represents the result of analyzing a single URL, including:
- Basic HTTP information (status code, redirects, etc.)
- Asset counts (documents, images, etc.)
- Deep analysis information (page type, dynamic loading, etc.)
- Potential issues detected

### `SummaryResult`
Represents the summary of analyzing multiple URLs, including:
- Total URLs analyzed
- Number of successful analyses
- Number of redirects
- Number of errors
- Number of URLs with and without assets

## Integration Points

The URL diagnostics system is integrated with the main application through:
1. The `ENABLE_URL_DIAGNOSTICS` environment variable
2. The `SpaceDataArchiveApp` class that adds the URL diagnostics panel to the UI
3. The `run.sh` script that starts the diagnostic service if needed

## Troubleshooting

If the URL diagnostics functionality is not working:

1. Check if the service is running:
   ```
   curl http://localhost:3000/health
   ```

2. Check the service logs:
   ```
   cat diagnostic-service.log
   ```

3. Make sure the Java application has the `ENABLE_URL_DIAGNOSTICS` environment variable set to `true` 
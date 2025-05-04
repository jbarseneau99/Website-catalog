# Webpage Classification System for Space Data Archive

## Overview

The Space Data Archive System now includes an AI-powered webpage classification capability that can automatically identify and categorize web content by type. Currently, the system can detect and distinguish between:

- **Press Releases** - Official announcements issued by organizations to the media
- **News Articles** - Journalistic content providing information about recent events, with optional categorization

## Features

- **Automatic Detection**: Analyzes webpage content to determine its type with confidence scoring
- **News Article Categorization**: For news articles, assigns one or more categories:
  - Space Exploration
  - Astronomy
  - Satellite Technology
  - Space Policy
  - Aerospace Industry
  - Scientific Research
  - Launches
  - Missions
  - General
- **Integration with Crawler**: Classification happens during the crawl process
- **Confidence Scoring**: Each classification includes a confidence value (0-1)
- **Batch Processing**: Can classify multiple pages efficiently

## Implementation

The classification system uses a combination of AI analysis and pattern recognition to determine webpage types. The main components include:

- **`webpage-classifier.js`** - Core classification logic
- **`ai-service.js`** - AI provider integration with Claude API
- **`dom-crawler.js`** - Integration with the crawling system

### Architecture

1. When a webpage is crawled, its content is extracted
2. The content is sent to the classifier
3. The classifier uses AI to analyze the content
4. Classification results are stored with the page metadata
5. Results can be used for filtering, searching, and organization

## Usage

### Enabling Classification

Classification is enabled by default in the updated system. You can control this with environment variables:

```bash
# Enable/disable classification
export ENABLE_WEBPAGE_CLASSIFICATION=true

# Enable/disable news categories
export CLASSIFY_NEWS_CATEGORIES=true
```

### Testing Classification

You can test the classification system using the included test script:

```bash
node src/test-webpage-classifier.js
```

### Programmatic Usage

```javascript
const webpageClassifier = require('./processing/webpage-classifier');

// Classify a webpage
const result = await webpageClassifier.classifyWebpage(url, content);

// Check the classification
if (result.type === webpageClassifier.WEBPAGE_TYPES.PRESS_RELEASE) {
  console.log("This is a press release");
} else if (result.type === webpageClassifier.WEBPAGE_TYPES.NEWS_ARTICLE) {
  console.log("This is a news article in categories:", result.categories);
}
```

## Data Model

Classification results follow this structure:

```javascript
{
  url: "https://example.com/some-page",
  type: "press_release", // or "news_article" or "unknown"
  confidence: 0.92, // 0-1 value
  categories: ["space_exploration", "launches"], // only for news_article
  classifiedAt: "2023-06-15T14:22:31.441Z"
}
```

## Technical Details

The classification system uses the Anthropic Claude API to analyze webpage content and determine its type. The system:

1. Extracts relevant content from the webpage
2. Creates a specialized prompt for the AI model
3. Parses the structured response from the AI
4. Maps the results to our classification schema
5. Stores the classification with appropriate metadata

## Future Enhancements

- Support for additional webpage types (technical papers, documentation, datasets)
- More granular categorization of press releases by topic
- Enhanced metadata extraction specific to each content type
- Integration with search functionality to filter by content type 
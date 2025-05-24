# WhatNext Project - Code Refactoring

## Overview
This project has been refactored to improve code quality, maintainability, and performance. The main focus was on the `BiletixScraper` class, which was responsible for scraping event data from the Biletix website.

## Improvements Made

### 1. Separation of Concerns
- Created utility classes to handle specific responsibilities:
  - `WebScraperUtils`: Handles web scraping operations (browser initialization, navigation, element interaction)
  - `EventUtils`: Handles event-related operations (creating events, saving events with duplicate checking)
  - `BiletixEventExtractor`: Handles extraction of event information from web elements
  - `BiletixScraperConfig`: Centralizes configuration values

### 2. Reduced Code Duplication
- Extracted common functionality into reusable methods
- Consolidated similar methods with slight variations into more generic methods

### 3. Improved Configuration Management
- Moved hardcoded values to a dedicated configuration class
- Added support for configuration via application properties
- Made constants more descriptive and organized

### 4. Enhanced Error Handling
- Added more robust error handling throughout the code
- Improved logging to provide better diagnostic information
- Made the code more resilient to failures (e.g., when clicking elements)

### 5. Performance Optimization
- Optimized the "Load More" button clicking process with multiple strategies
- Added proactive handling of overlays and cookie consent banners
- Improved browser resource management (opening/closing for each category)

### 6. Code Readability
- Removed unnecessary comments that just restated what the code was doing
- Added meaningful comments where they add value
- Used more descriptive method and variable names
- Made the code structure more consistent

## How to Use the Refactored Code

### Using the BiletixScraperRefactored Class
The `BiletixScraperRefactored` class is a drop-in replacement for the original `BiletixScraper` class. It implements the same `EventSource` interface and provides the same functionality, but with improved code quality.

To use it, simply update your dependency injection configuration to use `BiletixScraperRefactored` instead of `BiletixScraper`.

### Configuration
You can configure the scraper behavior through application properties:

```properties
# BiletixScraper Configuration
biletix.chrome.path=C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe
biletix.wait.timeout=60
biletix.load.more.attempts=20
biletix.load.more.wait=2000
```

### Utility Classes
The utility classes can be used independently for other scraping or event-related tasks:

#### WebScraperUtils
```java
// Initialize a browser
WebDriver driver = WebScraperUtils.initializeBrowser(chromePath, timeoutSeconds);

// Navigate to a URL
WebScraperUtils.navigateToUrl(driver, wait, url);

// Handle overlays that might intercept clicks
WebScraperUtils.handleOverlays(driver);
```

#### EventUtils
```java
// Create a concert event from basic info
ConcertEvent concert = EventUtils.createConcertEvent(eventInfo);

// Save an event with duplicate checking
EventUtils.saveConcertEventWithDuplicateChecking(concert, concertEventService, artistService, venueService);
```

#### BiletixEventExtractor
```java
// Extract basic info from event cards
List<Event> events = eventExtractor.extractBasicInfoFromCards(eventCards, category);

// Extract detailed info from an event page
DetailedEventInfo info = eventExtractor.extractDetailedInfo(driver);
```

## Future Improvements
- Add unit tests for the utility classes
- Implement caching to reduce database queries
- Add more configuration options for scraping behavior
- Improve error recovery mechanisms
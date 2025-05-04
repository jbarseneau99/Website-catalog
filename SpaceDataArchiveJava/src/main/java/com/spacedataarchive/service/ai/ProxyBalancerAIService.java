package com.spacedataarchive.service.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.spacedataarchive.model.AIMessage;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * AI service that assists with proxy management and rate limiting optimization.
 */
public class ProxyBalancerAIService {
    private static final Logger logger = LoggerFactory.getLogger(ProxyBalancerAIService.class);
    private static final String FUNCTION_ID = "proxy_balancer";
    
    private final AIMemoryService memoryService;
    private final AnthropicAIService aiService;
    
    /**
     * Constructor that initializes the proxy balancer AI service.
     * 
     * @param aiService The Anthropic AI service to use
     * @param memoryService The memory service to use
     */
    public ProxyBalancerAIService(AnthropicAIService aiService, AIMemoryService memoryService) {
        this.aiService = aiService;
        this.memoryService = memoryService;
        registerFunction();
    }
    
    /**
     * Registers this function with the memory service.
     */
    private void registerFunction() {
        memoryService.registerFunction(
            FUNCTION_ID,
            "Proxy Balancer",
            "Assists with proxy management and rate limiting optimization",
            AIMemoryService.AIFunctionType.FUNCTION
        );
        logger.info("Registered Proxy Balancer AI function");
    }
    
    /**
     * Analyzes a domain's rate limiting patterns to suggest optimal proxy configuration.
     * 
     * @param domain The domain to analyze
     * @param currentProxyCount Number of currently available proxies
     * @param scrapingHistory Optional scraping history data
     * @return Analysis results with proxy recommendations
     */
    public ProxyAnalysis analyzeProxyNeeds(String domain, int currentProxyCount, 
                                          List<Map<String, Object>> scrapingHistory) {
        ProxyAnalysis analysis = new ProxyAnalysis(domain, currentProxyCount);
        
        try {
            // Prepare history data for AI
            String historyData = formatScrapingHistory(scrapingHistory);
            
            // Generate prompt for AI
            String prompt = String.format(
                "I need to optimize proxy usage for scraping this domain: %s\n" +
                "Currently available proxies: %d\n\n" +
                "Recent scraping history details:\n%s\n\n" +
                "Please provide:\n" +
                "1. Recommended number of proxies for optimal performance\n" +
                "2. Rate limiting pattern analysis (requests per minute per IP)\n" +
                "3. Optimal rotation strategy (time-based, request-based, etc.)\n" +
                "4. Signs of proxy detection to watch for\n" +
                "5. Recovery strategies when rate limited\n" +
                "Format your response in a clear, concise structure with separate sections for each point.",
                domain, currentProxyCount, historyData
            );
            
            // Record prompt in memory
            memoryService.addMessage(FUNCTION_ID, new AIMessage(AIMessage.Role.USER, prompt));
            
            // Get AI response
            String aiResponse = aiService.sendMessage(prompt);
            
            // Record response in memory
            memoryService.addMessage(FUNCTION_ID, new AIMessage(AIMessage.Role.ASSISTANT, aiResponse));
            
            // Extract recommendations from AI response
            analysis.setRecommendedProxyCount(extractProxyCount(aiResponse));
            analysis.setRateLimitingPattern(extractRateLimitPattern(aiResponse));
            analysis.setRotationStrategy(extractRotationStrategy(aiResponse));
            analysis.setProxyDetectionSigns(extractDetectionSigns(aiResponse));
            analysis.setRecoveryStrategies(extractRecoveryStrategies(aiResponse));
            analysis.setFullAnalysis(aiResponse);
            
            return analysis;
            
        } catch (Exception e) {
            logger.error("Error analyzing proxy needs for domain: {}", domain, e);
            analysis.setError("Analysis error: " + e.getMessage());
            return analysis;
        }
    }
    
    /**
     * Formats scraping history data for AI prompt.
     * 
     * @param history The scraping history data
     * @return Formatted history string
     */
    private String formatScrapingHistory(List<Map<String, Object>> history) {
        if (history == null || history.isEmpty()) {
            return "No scraping history available.";
        }
        
        StringBuilder result = new StringBuilder();
        for (Map<String, Object> entry : history) {
            result.append("- Timestamp: ").append(entry.get("timestamp"))
                  .append(", Status: ").append(entry.get("status"))
                  .append(", Response Time: ").append(entry.get("responseTime"))
                  .append(" ms, IP: ").append(entry.get("proxyIp"))
                  .append("\n");
        }
        
        return result.toString();
    }
    
    /**
     * Extracts recommended proxy count from AI response.
     * 
     * @param aiResponse The AI response text
     * @return The recommended proxy count
     */
    private int extractProxyCount(String aiResponse) {
        Pattern pattern = Pattern.compile("recommend(?:ed)?\\s+(?:using|having)?\\s*(?:about|approximately)?\\s*(\\d+)\\s*(?:proxies|proxy)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(aiResponse);
        
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException e) {
                logger.warn("Failed to parse proxy count from AI response");
            }
        }
        
        // Second attempt with a simpler pattern
        pattern = Pattern.compile("(\\d+)\\s*proxies", Pattern.CASE_INSENSITIVE);
        matcher = pattern.matcher(aiResponse);
        
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException e) {
                logger.warn("Failed to parse proxy count from AI response (simple pattern)");
            }
        }
        
        // Return a reasonable default if extraction failed
        return 5;
    }
    
    /**
     * Extracts rate limiting pattern from AI response.
     * 
     * @param aiResponse The AI response text
     * @return The rate limiting pattern description
     */
    private String extractRateLimitPattern(String aiResponse) {
        // Find rate limiting section
        String[] lines = aiResponse.split("\n");
        StringBuilder pattern = new StringBuilder();
        boolean inRateLimitSection = false;
        
        for (String line : lines) {
            // Check if we've entered rate limiting section
            if (line.toLowerCase().contains("rate limit") && line.endsWith(":")) {
                inRateLimitSection = true;
                continue;
            }
            
            // Exit section when we hit another heading
            if (inRateLimitSection && line.trim().endsWith(":")) {
                break;
            }
            
            // Collect pattern description in the section
            if (inRateLimitSection && !line.trim().isEmpty()) {
                pattern.append(line.trim()).append("\n");
            }
        }
        
        return pattern.length() > 0 ? pattern.toString().trim() 
                                   : "Standard rate limiting, recommend 30-60 requests per hour per IP.";
    }
    
    /**
     * Extracts rotation strategy from AI response.
     * 
     * @param aiResponse The AI response text
     * @return The recommended rotation strategy
     */
    private String extractRotationStrategy(String aiResponse) {
        // Find rotation strategy section
        String[] lines = aiResponse.split("\n");
        StringBuilder strategy = new StringBuilder();
        boolean inRotationSection = false;
        
        for (String line : lines) {
            // Check if we've entered rotation strategy section
            if (line.toLowerCase().contains("rotation") && line.endsWith(":")) {
                inRotationSection = true;
                continue;
            }
            
            // Exit section when we hit another heading
            if (inRotationSection && line.trim().endsWith(":")) {
                break;
            }
            
            // Collect strategy in the section
            if (inRotationSection && !line.trim().isEmpty()) {
                strategy.append(line.trim()).append("\n");
            }
        }
        
        return strategy.length() > 0 ? strategy.toString().trim() 
                                    : "Rotate proxies after every 10 requests or 15 minutes, whichever comes first.";
    }
    
    /**
     * Extracts proxy detection signs from AI response.
     * 
     * @param aiResponse The AI response text
     * @return List of proxy detection signs
     */
    private List<String> extractDetectionSigns(String aiResponse) {
        List<String> signs = new ArrayList<>();
        
        // Find detection signs section
        String[] lines = aiResponse.split("\n");
        boolean inDetectionSection = false;
        
        for (String line : lines) {
            // Check if we've entered detection signs section
            if (line.toLowerCase().contains("detection") && line.endsWith(":")) {
                inDetectionSection = true;
                continue;
            }
            
            // Exit section when we hit another heading
            if (inDetectionSection && line.trim().endsWith(":")) {
                break;
            }
            
            // Collect signs in the section
            if (inDetectionSection && !line.trim().isEmpty()) {
                if (line.startsWith("-") || line.startsWith("*")) {
                    line = line.substring(1).trim();
                }
                signs.add(line.trim());
            }
        }
        
        // Default signs if none found
        if (signs.isEmpty()) {
            signs.add("CAPTCHA challenges appearing");
            signs.add("HTTP 429 (Too Many Requests) responses");
            signs.add("Unusually slow response times");
        }
        
        return signs;
    }
    
    /**
     * Extracts recovery strategies from AI response.
     * 
     * @param aiResponse The AI response text
     * @return List of recovery strategies
     */
    private List<String> extractRecoveryStrategies(String aiResponse) {
        List<String> strategies = new ArrayList<>();
        
        // Find recovery strategies section
        String[] lines = aiResponse.split("\n");
        boolean inRecoverySection = false;
        
        for (String line : lines) {
            // Check if we've entered recovery strategies section
            if (line.toLowerCase().contains("recovery") && line.endsWith(":")) {
                inRecoverySection = true;
                continue;
            }
            
            // Exit section when we hit another heading
            if (inRecoverySection && (line.trim().endsWith(":") && !line.toLowerCase().contains("recovery"))) {
                break;
            }
            
            // Collect strategies in the section
            if (inRecoverySection && !line.trim().isEmpty()) {
                if (line.startsWith("-") || line.startsWith("*")) {
                    line = line.substring(1).trim();
                }
                strategies.add(line.trim());
            }
        }
        
        // Default strategies if none found
        if (strategies.isEmpty()) {
            strategies.add("Implement exponential backoff when rate limited");
            strategies.add("Rotate to a fresh IP immediately when blocked");
            strategies.add("Reduce request frequency when detecting unusual responses");
        }
        
        return strategies;
    }
    
    /**
     * Class that represents proxy analysis results.
     */
    public static class ProxyAnalysis {
        private String domain;
        private int currentProxyCount;
        private int recommendedProxyCount;
        private String rateLimitingPattern;
        private String rotationStrategy;
        private List<String> proxyDetectionSigns = new ArrayList<>();
        private List<String> recoveryStrategies = new ArrayList<>();
        private String fullAnalysis;
        private String error;
        
        public ProxyAnalysis(String domain, int currentProxyCount) {
            this.domain = domain;
            this.currentProxyCount = currentProxyCount;
            this.recommendedProxyCount = currentProxyCount; // Default to current count
        }
        
        public String getDomain() {
            return domain;
        }
        
        public int getCurrentProxyCount() {
            return currentProxyCount;
        }
        
        public int getRecommendedProxyCount() {
            return recommendedProxyCount;
        }
        
        public void setRecommendedProxyCount(int recommendedProxyCount) {
            this.recommendedProxyCount = recommendedProxyCount;
        }
        
        public String getRateLimitingPattern() {
            return rateLimitingPattern;
        }
        
        public void setRateLimitingPattern(String rateLimitingPattern) {
            this.rateLimitingPattern = rateLimitingPattern;
        }
        
        public String getRotationStrategy() {
            return rotationStrategy;
        }
        
        public void setRotationStrategy(String rotationStrategy) {
            this.rotationStrategy = rotationStrategy;
        }
        
        public List<String> getProxyDetectionSigns() {
            return proxyDetectionSigns;
        }
        
        public void setProxyDetectionSigns(List<String> proxyDetectionSigns) {
            this.proxyDetectionSigns = proxyDetectionSigns;
        }
        
        public List<String> getRecoveryStrategies() {
            return recoveryStrategies;
        }
        
        public void setRecoveryStrategies(List<String> recoveryStrategies) {
            this.recoveryStrategies = recoveryStrategies;
        }
        
        public String getFullAnalysis() {
            return fullAnalysis;
        }
        
        public void setFullAnalysis(String fullAnalysis) {
            this.fullAnalysis = fullAnalysis;
        }
        
        public String getError() {
            return error;
        }
        
        public void setError(String error) {
            this.error = error;
        }
        
        public boolean hasError() {
            return error != null && !error.isEmpty();
        }
    }
} 
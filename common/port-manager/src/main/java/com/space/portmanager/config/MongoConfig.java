package com.space.portmanager.config;

import com.mongodb.MongoException;
import com.mongodb.MongoTimeoutException;
import com.mongodb.MongoSocketException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Configuration
public class MongoConfig extends AbstractMongoClientConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(MongoConfig.class);
    private static final int RETRY_ATTEMPTS = 3;
    private static final long RETRY_DELAY_MS = 5000;

    @Value("${spring.data.mongodb.uri}")
    private String mongoUri;

    @Value("${spring.data.mongodb.database}")
    private String database;

    @Override
    protected String getDatabaseName() {
        return database;
    }

    @Bean
    public MongoTemplate mongoTemplate() {
        try {
            MongoDatabaseFactory factory = mongoDbFactory();
            return new MongoTemplate(factory);
        } catch (MongoException e) {
            handleMongoException(e);
            throw e;
        }
    }

    @Bean
    public MongoDatabaseFactory mongoDbFactory() {
        int attempts = 0;
        MongoException lastException = null;

        while (attempts < RETRY_ATTEMPTS) {
            try {
                return new SimpleMongoClientDatabaseFactory(mongoUri);
            } catch (MongoException e) {
                lastException = e;
                attempts++;
                if (attempts < RETRY_ATTEMPTS) {
                    logger.warn("Failed to connect to MongoDB (attempt {}/{}). Retrying in {} ms...", 
                        attempts, RETRY_ATTEMPTS, RETRY_DELAY_MS);
                    try {
                        Thread.sleep(RETRY_DELAY_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        handleMongoException(lastException);
        throw lastException;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void testConnection() {
        try {
            mongoTemplate().getDb().runCommand(new org.bson.Document("ping", 1));
            logger.info("Successfully connected to MongoDB Atlas");
        } catch (MongoException e) {
            handleMongoException(e);
        }
    }

    private void handleMongoException(MongoException e) {
        String errorMessage;
        if (e instanceof MongoTimeoutException) {
            errorMessage = "Unable to connect to MongoDB Atlas - Connection timeout. Please check your network connection.";
        } else if (e instanceof MongoSocketException) {
            errorMessage = "Unable to connect to MongoDB Atlas - Network error. Please check your network connection and MongoDB Atlas status.";
        } else {
            errorMessage = String.format("MongoDB Atlas connection error: %s. Please check your connection string and credentials.", e.getMessage());
        }
        
        logger.error(errorMessage, e);
        System.err.println("\n=== MongoDB Connection Error ===");
        System.err.println(errorMessage);
        System.err.println("Please ensure:");
        System.err.println("1. Your MongoDB Atlas connection string is correct");
        System.err.println("2. Your network connection is stable");
        System.err.println("3. MongoDB Atlas service is running");
        System.err.println("4. IP whitelist includes your current IP address");
        System.err.println("===============================\n");
    }
} 
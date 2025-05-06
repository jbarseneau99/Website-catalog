#!/bin/bash

# MongoDB Atlas connection string (without database name)
MONGODB_URI_BASE="mongodb+srv://jbarseneau:Cheech99@cluster0.rd3fpku.mongodb.net"

# Function to create collections in a database
create_collections() {
    local db_name=$1
    echo "Initializing database: $db_name"
    
    # Create basic collections using mongosh
    mongosh "$MONGODB_URI_BASE/$db_name" --eval '
        // Create collections with validation
        db.createCollection("websites", {
            validator: {
                $jsonSchema: {
                    bsonType: "object",
                    required: ["url", "status", "createdAt"],
                    properties: {
                        url: { bsonType: "string" },
                        status: { bsonType: "string" },
                        createdAt: { bsonType: "date" },
                        processedAt: { bsonType: "date" },
                        nlpResults: { bsonType: "object" }
                    }
                }
            }
        });

        db.createCollection("nlp_results", {
            validator: {
                $jsonSchema: {
                    bsonType: "object",
                    required: ["websiteId", "createdAt"],
                    properties: {
                        websiteId: { bsonType: "objectId" },
                        createdAt: { bsonType: "date" },
                        keywords: { bsonType: "array" },
                        summary: { bsonType: "string" },
                        sentiment: { bsonType: "object" }
                    }
                }
            }
        });

        db.createCollection("validation_results", {
            validator: {
                $jsonSchema: {
                    bsonType: "object",
                    required: ["websiteId", "createdAt", "isValid"],
                    properties: {
                        websiteId: { bsonType: "objectId" },
                        createdAt: { bsonType: "date" },
                        isValid: { bsonType: "bool" },
                        statusCode: { bsonType: "int" },
                        errorMessage: { bsonType: "string" }
                    }
                }
            }
        });

        // Create indexes
        db.websites.createIndex({ "url": 1 }, { unique: true });
        db.websites.createIndex({ "status": 1 });
        db.websites.createIndex({ "createdAt": 1 });
        
        db.nlp_results.createIndex({ "websiteId": 1 });
        db.nlp_results.createIndex({ "createdAt": 1 });
        
        db.validation_results.createIndex({ "websiteId": 1 });
        db.validation_results.createIndex({ "createdAt": 1 });
        
        print("Database", db.getName(), "initialized successfully");
    '
}

echo "Starting database initialization..."

# Create development database
create_collections "website-catalog"

# Create staging database
create_collections "website-catalog-staging"

# Create production database
create_collections "website-catalog-prod"

echo "All databases initialized successfully" 
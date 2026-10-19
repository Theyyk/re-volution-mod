package com.example.revolution.persistence;

import com.example.revolution.config.BrandingProperties;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.apache.logging.log4j.Logger;
import org.bson.Document;

/** Owns the server's Mongo client. No gameplay state or document mapping lives here. */
public final class MongoConnection implements AutoCloseable {
    private final Logger logger;
    private MongoClient client;
    private MongoDatabase database;

    public MongoConnection(Logger logger) { this.logger = logger; }

    public synchronized void connect() {
        if (client != null) return;
        try {
            client = MongoClients.create(BrandingProperties.get("revolution.mongo.uri", "mongodb://localhost:27017"));
            database = client.getDatabase(BrandingProperties.get("revolution.mongo.database", "MyProject_build"));
            logger.info("MongoDB connected!");
        } catch (RuntimeException e) {
            close();
            logger.error("MongoDB error: " + e.getMessage(), e);
        }
    }

    public boolean isConnected() { return database != null; }

    public MongoDatabase database() {
        if (database == null) connect();
        if (database == null) throw new IllegalStateException("MongoDB connection is unavailable");
        return database;
    }

    public MongoCollection<Document> collection(String name) { return database().getCollection(name); }

    @Override public synchronized void close() {
        MongoClient closing = client;
        client = null;
        database = null;
        if (closing != null) closing.close();
    }
}

package com.stupidrepo.mcscanner;

import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import java.util.logging.Level;
import java.util.logging.Logger;

public class DatabaseHandler {
    public MongoDatabase mainDatabase;
    private final Logger logger = Logger.getLogger("com.stupidrepo.mcscanner");

    public DatabaseHandler(String uri) {
        try (MongoClient mongoClient = MongoClients.create(uri)) {
            mainDatabase = mongoClient.getDatabase("MCScanner");
            logger.log(Level.INFO, "Connected to database: '" + mainDatabase.getName() + "'");
        }
    }

    public void writeDetailsToDB(String ip, String version, String motd, int maxPlayers) {
        try {
            mainDatabase.getCollection("servers")
                    .insertOne(
                            new Document("ip", ip)
                                    .append("version", version)
                                    .append("motd", motd)
                                    .append("maxPlayers", maxPlayers)
                    );
            logger.log(Level.INFO, "Added " + ip + " to database.");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to add " + ip + " to database.");
        }
    }

    public void writeDetailsToDB(String ip, String motd, int maxPlayers) {
        try {
            mainDatabase.getCollection("servers")
                    .insertOne(
                            new Document("ip", ip)
                                    .append("motd", motd)
                                    .append("maxPlayers", maxPlayers)
                    );
            logger.log(Level.WARNING, "[LEGACY!] Added " + ip + " to database.");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "[LEGACY!] Failed to add " + ip + " to database.");
        }
    }

    public boolean isIPInDB(String ip) {
        Document myDoc = mainDatabase.getCollection("servers").find(new Document("ip", ip)).first();
        return myDoc != null;
    }
}
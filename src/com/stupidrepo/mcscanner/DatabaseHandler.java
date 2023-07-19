package com.stupidrepo.mcscanner;

import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DatabaseHandler {
    public MongoClient mainMongoClient;
    public MongoDatabase mainDatabase;
    private MongoCollection<Document> mainCollection;

    private final Logger logger = Logger.getLogger("com.stupidrepo.mcscanner");

    public DatabaseHandler(String uri) {
        try {
            mainMongoClient = MongoClients.create(uri);
            mainDatabase = mainMongoClient.getDatabase("MCScanner");
            mainCollection = mainDatabase.getCollection("servers");
            logger.log(Level.INFO, "Connected to database: '" + mainDatabase.getName() + "'");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to connect to database!");
        }
    }

    public ArrayList<Document> getServers() {
        ArrayList<Document> servers = new ArrayList<>();
        try {
            for (Document doc : mainCollection.find()) {
                servers.add(doc);
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to get servers from database!");
        }

        return servers;
    }

    public Long getServerCount() {
        try {
            return mainCollection.countDocuments();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to get server count from database!");
            return null;
        }
    }

    public void writeDetailsToDB(String ip, String version, String motd, int maxPlayers) {
        try {
            mainCollection
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
            mainCollection
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
        try {
            Document myDoc = mainCollection.find(new Document("ip", ip)).first();
            return myDoc != null;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to check if " + ip + " is in database.");
            return false;
        }
    }
}
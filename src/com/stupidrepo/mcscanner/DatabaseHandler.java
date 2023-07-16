package com.stupidrepo.mcscanner;

import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

public class DatabaseHandler {
    public MongoDatabase mainDatabase;

    public DatabaseHandler(String uri) {
        try (MongoClient mongoClient = MongoClients.create(uri)) {
            mainDatabase = mongoClient.getDatabase("MCScanner");
            System.out.println("Connected to database: '" + mainDatabase.getName() + "'");
        }
    }

    public void writeDetailsToDB(String ip, String version, String motd, int maxPlayers) {
        mainDatabase.getCollection("servers")
                .insertOne(
                        new Document("ip", ip)
                                .append("version", version)
                                .append("motd", motd)
                                .append("maxPlayers", maxPlayers)
                );
        System.out.println("Added " + ip + " to database.");
    }

    public void writeDetailsToDB(String ip, String motd, int maxPlayers) {
        mainDatabase.getCollection("servers")
                .insertOne(
                        new Document("ip", ip)
                                .append("motd", motd)
                                .append("maxPlayers", maxPlayers)
                );
        System.out.println("Added " + ip + " [legacy] to database.");
    }

    public boolean isIPInDB(String ip) {
        Document myDoc = mainDatabase.getCollection("servers").find(new Document("ip", ip)).first();
        if (myDoc != null) {
            System.out.println("Found " + ip + " in database.");
            return true;
        } else {
            System.out.println("Did not find " + ip + " in database.");
            return false;
        }
    }
}
package com.stupidrepo.mcscanner;

import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.BsonDateTime;
import org.bson.Document;

import java.util.ArrayList;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DatabaseHandler {
    public MongoClient mainMongoClient;
    public MongoDatabase mainDatabase;
    private MongoCollection<Document> mainCollection;

    private final Logger logger = Logger.getLogger("com.stupidrepo.mcscanner");

    /**
     * Initiates a new DatabaseHandler.
     *
     * @param uri The URI to the MongoDB Database
     */
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

    /**
     * Gets all the servers in the database.
     */
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

//    /**
//     * Gets the server count from the database.
//     */
//    public Long getServerCount() {
//        try {
//            return mainCollection.countDocuments();
//        } catch (Exception e) {
//            logger.log(Level.SEVERE, "Failed to get server count from database!");
//            return null;
//        }
//    }

    /**
     * Writes an entry to the database.
     *
     * @param ip The IP of the server (<code>192.168.0.1</code>)
     * @param version The version of the server (<code>1.20.1</code>)
     * @param motd The MOTD of the server (<code>A Minecraft Server</code>)
     * @param maxPlayers The max players of the server (<code>20</code>)
     */
    public void writeDetailsToDB(String ip, String version, String motd, int maxPlayers) {
        try {
            mainCollection
                    .insertOne(
                            new Document("ip", ip)
                                    .append("version", version)
                                    .append("motd", motd)
                                    .append("maxPlayers", maxPlayers)
                                    .append("lastUpdated", new BsonDateTime(new Date().getTime()))
                    );
            logger.log(Level.INFO, "Added " + ip + " to database.");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to add " + ip + " to database.");
        }
    }

    /**
     * Updates a server in the database.
     *
     * @param ip The IP of the server
     * @param version The version of the server
     * @param motd The MOTD of the server
     * @param maxPlayers The max players of the server
     */
    public void updateServerByIPInDB(String ip, String version, String motd, int maxPlayers) {
        try {
            mainCollection.updateOne(
                    new Document("ip", ip),
                    new Document("$set", new Document("version", version)
                            .append("motd", motd)
                            .append("maxPlayers", maxPlayers)
                            .append("lastUpdated", new BsonDateTime(new Date().getTime()))
                    )
            );
            logger.log(Level.INFO, "Updated " + ip + " in database.");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to update " + ip + " in database.");
            logger.log(Level.SEVERE, e.getMessage());
        }
    }

    /**
     * Checks if a server is in the database.
     *
     * @param ip The IP of the server
     */
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
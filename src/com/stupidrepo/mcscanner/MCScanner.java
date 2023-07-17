package com.stupidrepo.mcscanner;

import java.awt.BorderLayout;
import java.awt.event.WindowEvent;
import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;

public class MCScanner {
    public static void main(String[] var0) throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
        AtomicInteger threads = new AtomicInteger(1024);
        int timeout = 1000;
        int minimumRange = 1;
        int maxRange = 255;
        int port = 25565;

        // TODO: Optimize all of this code.
        Logger logger = Logger.getLogger("com.stupidrepo.mcscanner");

        float version = 1.14f;

        AtomicReference<String> uri = new AtomicReference<>("mongodb://localhost:27017");

        PopupHandler threadsPopup = new PopupHandler("How many threads would you like to use?", "1024", "OK");
        threadsPopup.showAndWait();

        try {
            threads.set(Integer.parseInt(threadsPopup.responseText));
        } catch (NumberFormatException e) {
            logger.log(Level.SEVERE, "Invalid thread count.");
        }

        PopupHandler mongoDBURIPopup = new PopupHandler("Enter the URI to the MongoDB database:", "mongodb://localhost:27017", "Done");
        mongoDBURIPopup.showAndWait();

        uri.set(mongoDBURIPopup.responseText);

        DatabaseHandler databaseHandler = new DatabaseHandler(uri.get());

        logger.log(Level.INFO, "Scanning IPs...");

        JFrame frame = new JFrame("MCScanner v" + version);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setSize(300, 100);
        frame.setLayout(new BorderLayout());

        double progressThing = (maxRange - minimumRange + 1) * 256 * 256;

        ArrayList < Thread > threadList = new ArrayList < Thread > ();

        JLabel scannedLabel = new JLabel("Scanned: 0/" + progressThing);
        scannedLabel.setHorizontalAlignment(0);

        frame.add(scannedLabel, "Center");

        long scanned = 0;

        frame.setVisible(true);

        ExecutorService executor = Executors.newFixedThreadPool(threads.get());

        for (int i = minimumRange; i <= maxRange; ++i) {
            for (int j = 0; j <= 255; ++j) {
                for (int k = 0; k <= 255; ++k) {
                    // for (int l = 0; l <= 255; ++l) {
                        String ip = i + "." + j + "." + k + ".0";
        
                        Thread scannerThread = new Thread(new ScannerThread(ip, port, timeout, databaseHandler));
                        threadList.add(scannerThread);
                        executor.execute(scannerThread);
        
                        if (threadList.size() >= threads.get()) {
                            for (Thread nextThread: threadList) {
                                try {
                                    nextThread.join();
                                    ++scanned;
                                    scannedLabel.setText("Scanned: " + scanned + "/" + progressThing + " (" + Math.round((scanned / progressThing) * 100) / 100 + "%)");
                                } catch (InterruptedException timeout2) {
                                    // eh
                                }
                            }
                            threadList.clear();
                        }
                    // }
                }
            }
        }

        try {
            executor.shutdown();
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            logger.log(Level.SEVERE, "Interrupted while waiting for threads to finish.");
        }

        frame.setVisible(false);
        frame.dispatchEvent(new WindowEvent(frame, 201));
        logger.log(Level.INFO, "Scan completed!");
    }
}

class ScannerThread implements Runnable {
    private final String ip;
    private final int port;
    private final int timeout;
    private final DatabaseHandler dbHandler;

    public boolean didHit = false;

    public ScannerThread(String ip, int port, int timeout, DatabaseHandler dbHandler) {
        this.ip = ip;
        this.port = port;
        this.timeout = timeout;
        this.dbHandler = dbHandler;
    }

    public void run() {
        try {
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress(this.ip, this.port), this.timeout);

            InputStream inputStream = socket.getInputStream();
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_16BE);

            OutputStream outputStream = socket.getOutputStream();

            outputStream.write(new byte[] {
                    (byte) 0xFE, (byte) 0x01
            });

            socket.setSoTimeout(this.timeout);

            int packetId = inputStream.read();

            if (packetId == -1) {
                socket.close();
                throw new IOException("Premature end of stream.");
            }

            if (packetId != 0xFF) {
                socket.close();
                throw new IOException("Invalid packet ID (" + packetId + ").");
            }

            int length = inputStreamReader.read();

            if (length == -1) {
                socket.close();
                throw new IOException("Premature end of stream.");
            }

            if (length == 0) {
                socket.close();
                throw new IOException("Invalid string length.");
            }

            char[] chars = new char[length];

            if (inputStreamReader.read(chars, 0, length) != length) {
                socket.close();
                throw new IOException("Premature end of stream.");
            }

            if(dbHandler.isIPInDB(ip)) {
                socket.close();
                return;
            }

            String string = new String(chars);

            String[] data;
            if (string.startsWith("§")) {
                // This is for 1.8 and above
                data = string.split("\0");

                dbHandler.writeDetailsToDB(ip, data[2], data[3], Integer.parseInt(data[5]));
            } else {
                // This is for 1.7 and below
                data = string.split("§");

                dbHandler.writeDetailsToDB(ip, data[0], Integer.parseInt(data[2]));
            }

            socket.close();

            didHit = true;
        } catch (IOException var8) {
            // No response/invalid response/timeout/port accidentally left open
        }
    }
}
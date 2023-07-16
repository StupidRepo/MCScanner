package com.stupidrepo.mcscanner;

import java.awt.BorderLayout;
import java.awt.event.WindowEvent;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;

public class MCScanner {
    public static void main(String[] var0) {
        AtomicInteger threads = new AtomicInteger(1024);
        int timeout = 1000;
        int minimumRange = 1;
        int maxRange = 255;
        int port = 25565;

        // TODO: Optimize all of this code.
        Logger logger = Logger.getLogger("com.stupidrepo.mcscanner");

        float version = 1.12f;

        AtomicReference<String> uri = new AtomicReference<>("mongodb://localhost:27017");

        JFrame threadFrame = new JFrame("MCScanner v" + version);
        threadFrame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        threadFrame.setSize(500, 125);
        threadFrame.setLayout(new BorderLayout());

        JLabel threadLabel = new JLabel("Threads to use (default is 1024, recommended is 1024-2048):");
        threadLabel.setHorizontalAlignment(0);

        threadFrame.add(threadLabel, "North");

        JTextField threadField = new JTextField("1024");
        threadField.setHorizontalAlignment(0);

        threadFrame.add(threadField, "Center");

        JButton threadButton = new JButton("OK");
        threadFrame.add(threadButton, "East");

        JButton quitButton = new JButton("Quit");
        threadFrame.add(quitButton, "South");

        threadButton.addActionListener(e -> {
            try {
                Integer.parseInt(threadField.getText());
            } catch (NumberFormatException exception) {
                JOptionPane.showMessageDialog(null, "Invalid number.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            threads.set(Integer.parseInt(threadField.getText()));
            threadFrame.setVisible(false);
            threadFrame.dispatchEvent(new WindowEvent(threadFrame, 201));
        });

        quitButton.addActionListener(e -> {
            threadFrame.setVisible(false);
            threadFrame.dispatchEvent(new WindowEvent(threadFrame, 201));
            System.exit(0);
        });

        threadFrame.setVisible(true);

        while(threadFrame.isVisible()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        JFrame mongoFrame = new JFrame("MCScanner v" + version);
        mongoFrame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        mongoFrame.setSize(500, 125);
        mongoFrame.setLayout(new BorderLayout());

        JLabel mongoLabel = new JLabel("MongoDB URI (default is mongodb://localhost:27017):");
        mongoLabel.setHorizontalAlignment(0);

        mongoFrame.add(mongoLabel, "North");

        JTextField mongoField = new JTextField("mongodb://localhost:27017");
        mongoField.setHorizontalAlignment(0);

        mongoFrame.add(mongoField, "Center");

        JButton mongoButton = new JButton("OK");
        mongoFrame.add(mongoButton, "East");

        JButton mQuitButton = new JButton("Quit");
        mongoFrame.add(mQuitButton, "South");

        mongoButton.addActionListener(e -> {
            uri.set(mongoField.getText());
            mongoFrame.setVisible(false);
            mongoFrame.dispatchEvent(new WindowEvent(threadFrame, 201));
        });

        mQuitButton.addActionListener(e -> {
            mongoFrame.setVisible(false);
            mongoFrame.dispatchEvent(new WindowEvent(threadFrame, 201));
            System.exit(0);
        });

        mongoFrame.setVisible(true);

        while(mongoFrame.isVisible()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        DatabaseHandler databaseHandler = new DatabaseHandler(uri.get());

        logger.log(Level.INFO, "Scanning IPs...");

        JFrame frame = new JFrame("MCScanner v" + version);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setSize(300, 100);
        frame.setLayout(new BorderLayout());

        double progressThing = (maxRange - minimumRange + 1) * 256 * 256;
        ArrayList < Thread > threadList = new ArrayList < Thread > ();

        JLabel scannedLabel = new JLabel("Scanned: 0/" + progressThing * 256);
        scannedLabel.setHorizontalAlignment(0);

        frame.add(scannedLabel, "Center");
        frame.setVisible(true);

        long scanned = 0;

        // TODO: Make this whole thing more efficient, and less ugly.
        for (int i = minimumRange; i <= maxRange; ++i) {
            for (int j = 0; j <= 255; ++j) {
                for (int k = 0; k <= 255; ++k) {
                    for (int l = 0; l <= 255; ++l) {
                        String ip = i + "." + j + "." + k + "." + l;

                        Thread scanThread = new Thread(new ScannerThread(ip, port, timeout, databaseHandler));
                        threadList.add(scanThread);
                        scanThread.start();

                        if (threadList.size() >= threads.get()) {
                            for (Thread nextThread: threadList) {
                                try {
                                    nextThread.join();
                                    ++scanned;
                                    // progressBar.setValue(scanned);
                                    scannedLabel.setText("Scanned: " + scanned + "/" + progressThing * 256 + " (" + Math.round((scanned / progressThing * 256) * 100) / 100 + "%)");
                                } catch (InterruptedException timeout2) {
                                    // eh
                                }
                            }
                            threadList.clear();
                        }
                    }
                }
            }
        }

        for (Thread nextThreadAgain: threadList) {
            try {
                nextThreadAgain.join();
                ++scanned;
                // progressBar.setValue(scanned);
                scannedLabel.setText("Scanned: " + scanned + "/" + progressThing * 256);
            } catch (InterruptedException timeout1) {
                // well
            }
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
        } catch (IOException var8) {
            // No response/invalid response/timeout/port accidentally left open
        }
    }
}
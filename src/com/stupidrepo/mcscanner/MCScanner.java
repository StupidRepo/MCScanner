package com.stupidrepo.mcscanner;

import java.awt.BorderLayout;
import java.awt.event.WindowEvent;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.xml.crypto.Data;

public class MCScanner {
    public static void main(String[] var0) {
        int threads = 1024;
        int timeout = 1000;
        int minimumRange = 1;
        int maxRange = 255;
        int port = 25565;

        // TODO: Optimize all this code.

        Logger logger = Logger.getLogger("com.stupidrepo.mcscanner");

        float version = 1.10f;

        DatabaseHandler databaseHandler = new DatabaseHandler("mongodb://localhost:27017");

        logger.log(Level.INFO, "Scanning IPs...");

        JFrame frame = new JFrame("MCScanner v" + version);
        frame.setDefaultCloseOperation(3);
        frame.setSize(300, 100);
        frame.setLayout(new BorderLayout());

        // JProgressBar progressBar = new JProgressBar(0, (int)progressThing);
        // progressBar.setStringPainted(true);

        // frame.add(progressBar, "Center");

        double progressThing = (maxRange - minimumRange + 1) * 256 * 256;
        ArrayList < Thread > threadList = new ArrayList < Thread > ();

        JLabel scannedLabel = new JLabel("Scanned: 0/" + progressThing * 256);
        scannedLabel.setHorizontalAlignment(0);

        // frame.add(scannedLabel, "South");

        frame.add(scannedLabel, "Center");
        frame.setVisible(true);

        long scanned = 0;

        for (int i = minimumRange; i <= maxRange; ++i) {
            for (int j = 0; j <= 255; ++j) {
                for (int k = 0; k <= 255; ++k) {
                    for (int l = 0; l <= 255; ++l) {
                        // String ip = "" + i + "." + j + "." + k + ".0";
                        String ip = i + "." + j + "." + k + "." + l;

                        Thread scanThread = new Thread(new ScannerThread(ip, port, timeout, databaseHandler));
                        threadList.add(scanThread);
                        scanThread.start();

                        if (threadList.size() >= threads) {
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

            OutputStream outp = socket.getOutputStream();

            outp.write(new byte[] {
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
                System.out.println("IP is in database, skipping...");
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
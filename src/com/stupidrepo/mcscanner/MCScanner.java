package com.stupidrepo.mcscanner;

import java.awt.BorderLayout;
import java.awt.GridLayout;
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
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import org.bson.Document;

public class MCScanner {
    public static void main(String[] var0) {
        AtomicInteger threads = new AtomicInteger(1024);
        int timeout = 1000;
        int minimumRange = 1;
        int maxRange = 255;
        int port = 25565;

        Logger logger = Logger.getLogger("com.stupidrepo.mcscanner");

        float version = 1.15f;

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

        JLabel scannedLabel = new JLabel("Scanned: 0/" + progressThing * 256);
        scannedLabel.setHorizontalAlignment(0);

        frame.add(scannedLabel, "Center");

        long scanned = 0;
        long hits = 0;

        JLabel hitsLabel = new JLabel("Hits: 0/0");
        hitsLabel.setHorizontalAlignment(0);

        frame.add(hitsLabel, "South");

        JButton viewServersButton = new JButton("View Servers");

        viewServersButton.addActionListener(e -> {
            ServerList serverList = new ServerList(databaseHandler);
            serverList.showGUI();
        });

        frame.add(viewServersButton, "North");

        frame.setVisible(true);

        // TODO: Make this whole thing more efficient, and less ugly. [1]
        for (int i = minimumRange; i <= maxRange; ++i) {
            for (int j = 0; j <= 255; ++j) {
                for (int k = 0; k <= 255; ++k) {
                    for (int l = 0; l <= 255; ++l) {
                        String ip = i + "." + j + "." + k + "." + l;

                        ScannerThread scannerThread = new ScannerThread(ip, port, timeout, databaseHandler);
                        Thread scanThread = new Thread(scannerThread);
                        threadList.add(scanThread);
                        scanThread.start();

                        if (threadList.size() >= threads.get()) {
                            for (Thread nextThread: threadList) {
                                try {
                                    nextThread.join();
                                    ++scanned;
                                    if(scannerThread.didHit) {
                                        ++hits;
                                    }
                                    // progressBar.setValue(scanned);
                                    hitsLabel.setText("Hits: " + hits + "/" + scanned);
                                    scannedLabel.setText("Scanned: " + scanned + "/" + progressThing * 256 + " (" + Math.round((scanned / (progressThing * 256)) * 100) / 100 + "%)");
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
                hitsLabel.setText("Hits: " + hits + "/" + scanned);
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

class ServerList {
    private final DatabaseHandler dbHandler;

    public ServerList(DatabaseHandler dbHandler) {
        this.dbHandler = dbHandler;
    }

    public void showGUI() {
        JFrame frame = new JFrame("MCScanner - Servers");
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        frame.setSize(500, 500);
        frame.setLayout(new BorderLayout());
        
        JTable table = new JTable();
        table.setModel(new DefaultTableModel(
            new Object[][] {
            },
            new String[] {
                "IP", "MOTD", "Version", "Max Players"
            }
        ));
        table.getColumnModel().getColumn(0).setPreferredWidth(100);
        table.getColumnModel().getColumn(1).setPreferredWidth(200);
        table.getColumnModel().getColumn(2).setPreferredWidth(50);
        table.getColumnModel().getColumn(3).setPreferredWidth(50);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        table.setFillsViewportHeight(true);
        table.setRowHeight(20);
        table.setRowSelectionAllowed(false);
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setResizingAllowed(false);
        
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        ArrayList < Document > documents = dbHandler.getServers();
        for (Document document: documents) {
            String ip = document.getString("ip");
            String motd = document.getString("motd");
            String version = document.getString("version");
            int players = document.getInteger("maxPlayers");

            ((DefaultTableModel) table.getModel()).addRow(new Object[] {
                ip, motd, version, players
            });
        }

        // refresh option
        JButton refreshButton = new JButton("Refresh");

        refreshButton.addActionListener(e -> {
            ((DefaultTableModel) table.getModel()).setRowCount(0);

            ArrayList < Document > documents1 = dbHandler.getServers();
            for (Document document: documents1) {
                String ip = document.getString("ip");
                String motd = document.getString("motd");
                String version = document.getString("version");
                int players = document.getInteger("maxPlayers");

                ((DefaultTableModel) table.getModel()).addRow(new Object[] {
                    ip, motd, version, players
                });
            }
        });
        
        TableRowSorter < TableModel > sorter = new TableRowSorter < > (table.getModel());
        table.setRowSorter(sorter);

        // frame.add(table, BorderLayout.CENTER);
        frame.add(scrollPane, BorderLayout.CENTER);
        frame.add(refreshButton, BorderLayout.SOUTH);
        frame.setVisible(true);
    }
}
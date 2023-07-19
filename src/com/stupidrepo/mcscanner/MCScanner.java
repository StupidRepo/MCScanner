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
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import org.bson.Document;

public class MCScanner {
    private static int offsetI = 1;
    private static int offsetJ = 0;
    private static int offsetK = 0;
    private static int offsetL = 0;
    private static boolean stopping = false;

    public static void main(String[] var0) {
        AtomicInteger threads = new AtomicInteger(1024);
        int timeout = 1000;
        int minimumRange = 1;
        int maxRange = 255;
        int port = 25565;

        Logger logger = Logger.getLogger("com.stupidrepo.mcscanner");

        float version = 1.17f;

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
        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        frame.setSize(300, 100);
        frame.setLayout(new BorderLayout());

        double progressThing = (maxRange - minimumRange + 1) * 256 * 256;
        ArrayList < Thread > threadList = new ArrayList < Thread > ();

        JLabel scannedLabel = new JLabel("Scanned: 0/" + progressThing * 256);
        scannedLabel.setHorizontalAlignment(0);

        frame.add(scannedLabel, "Center");

        long scanned = 0;

        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                stopping = true;
                logger.log(Level.INFO, "Stopping threads...");
                for (Thread nextThread: threadList) {
                    try {
                        nextThread.join();
                    } catch (InterruptedException timeouter) {
                        // Timeout or something idk
                    }
                }
                logger.log(Level.INFO, "Making an '.mcscanner'...");
                try {
                    BufferedWriter writer = new BufferedWriter(new FileWriter(".mcscanner"));
                    writer.write(String.valueOf(offsetI + "\n" + offsetJ + "\n" + offsetK + "\n" + offsetL));
                    writer.close();
                } catch (IOException e) {
                    logger.log(Level.SEVERE, "Failed to write '.mcscanner'!");
                }
                logger.log(Level.INFO, "Exiting...");
                System.exit(0);
            }
        });

        ServerList serverList = new ServerList(databaseHandler);

        JButton viewServersButton = new JButton("Show Server List");

        viewServersButton.addActionListener(e -> {
            if(serverList.toggleGUI()) {
                viewServersButton.setText("Hide Server List");
            } else {
                viewServersButton.setText("Show Server List");
            }
        });

        frame.add(viewServersButton, "North");

        frame.setVisible(true);

        File offsetFile = new File(".mcscanner");
        if (offsetFile.exists()) {
            try {
                logger.log(Level.INFO, "Found '.mcscanner'!");
                BufferedReader reader = new BufferedReader(new FileReader(offsetFile));
                offsetI = Integer.parseInt(reader.readLine());
                offsetJ = Integer.parseInt(reader.readLine());
                offsetK = Integer.parseInt(reader.readLine());
                offsetL = Integer.parseInt(reader.readLine());
                logger.log(Level.INFO, "Continuing from " + offsetI + "." + offsetJ + "." + offsetK + "." + offsetL + "...");
                reader.close();
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Failed to read '.mcscanner'!");
            }
        }

        int thisOffsetI = offsetI;
        int thisOffsetJ = offsetJ;
        int thisOffsetK = offsetK;
        int thisOffsetL = offsetL;
        for (int i = thisOffsetI; i <= maxRange; ++i) {
            if(stopping) {
                break;
            } else {
                offsetI = i;
            }
            for (int j = thisOffsetJ; j <= 255; ++j) {
                if(stopping) {
                    break;
                } else {
                    offsetJ = j;
                }
                for (int k = thisOffsetK; k <= 255; ++k) {
                    if(stopping) {
                        break;
                    } else {
                        offsetK = k;
                    }
                    for (int l = thisOffsetL; l <= 255; ++l) {
                        if(stopping) {
                            break;
                        } else {
                            offsetL = l;
                            String ip = i + "." + j + "." + k + "." + l;

                            ScannerThread scannerThread = new ScannerThread(ip, port, timeout, databaseHandler);
                            Thread scanThread = new Thread(scannerThread);
                            threadList.add(scanThread);
                            scanThread.start();
                        }

                        if (threadList.size() >= threads.get()) {
                            for (Thread nextThread: threadList) {
                                try {
                                    nextThread.join();
                                    ++scanned;
                                    scannedLabel.setText("Scanned: " + scanned + "/" + progressThing * 256 + " (" + Math.round((scanned / (progressThing * 256)) * 100) / 100 + "%)");
                                } catch (InterruptedException timeout2) {
                                    // Timed out or smth
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
                scannedLabel.setText("Scanned: " + scanned + "/" + progressThing * 256);
            } catch (InterruptedException timeout1) {
                // Timeout, again!
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
            if(dbHandler.isIPInDB(ip)) {
                return;
            }
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

            if (packetId == -1 || packetId != 0xFF) {
                socket.close();
                throw new IOException("Invalid packet: (" + packetId + ")");
            }

            int length = inputStreamReader.read();

            if (length == -1 || length == 0) {
                socket.close();
                throw new IOException("Premature end of stream/invalid String length.");
            }

            char[] chars = new char[length];

            if (inputStreamReader.read(chars, 0, length) != length) {
                socket.close();
                throw new IOException("Premature end of stream.");
            }

            String string = new String(chars);

            String[] data;
            if (string.startsWith("§")) {
                data = string.split("\0");

                dbHandler.writeDetailsToDB(ip, data[2], data[3], Integer.parseInt(data[5]));
            } else {
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
    private final JFrame frame;

    public ServerList(DatabaseHandler dbHandler) {
        this.dbHandler = dbHandler;
        this.frame = new JFrame("MCScanner - Servers (" + this.dbHandler.getServerCount() + ")");
        this.frame.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
        this.frame.setSize(500, 500);
        this.frame.setLayout(new BorderLayout());
        
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

        ArrayList < Document > documents = this.dbHandler.getServers();
        for (Document document: documents) {
            String ip = document.getString("ip");
            String motd = document.getString("motd");
            String version = document.getString("version");
            int players = document.getInteger("maxPlayers");

            ((DefaultTableModel) table.getModel()).addRow(new Object[] {
                ip, motd, version, players
            });
        }

        JButton refreshButton = new JButton("Refresh");

        refreshButton.addActionListener(e -> {
            ((DefaultTableModel) table.getModel()).setRowCount(0);

            ArrayList < Document > documents1 = this.dbHandler.getServers();
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

        this.frame.add(scrollPane, BorderLayout.CENTER);
        this.frame.add(refreshButton, BorderLayout.SOUTH);
    }

    public boolean toggleGUI() {
        this.frame.setVisible(!this.frame.isVisible());
        return this.frame.isVisible();
    }
}
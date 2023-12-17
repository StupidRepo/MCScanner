package com.stupidrepo.mcscanner;

import com.stupidrepo.mcscanner.language.LanguageHandler;
import org.bson.Document;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.PatternSyntaxException;

public class MCScanner {
    private static int offsetI = 1;
    private static int offsetJ = 0;
    private static int offsetK = 0;
    private static int offsetL = 0;
    private static boolean stopping = false;

    private static String ip = "...";

    public static LanguageHandler lang;
    public static SessionManager session;

    public static void main(String[] args) throws InterruptedException {
        AtomicInteger threads = new AtomicInteger(1024);
        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(threads.get());
        int timeout = 1000;
        int maxRange = 255;
        int port = 25565;

        Logger logger = Logger.getLogger("com.stupidrepo.mcscanner");

        AtomicReference<String> uri = new AtomicReference<>("mongodb://localhost:27017");

        lang = new LanguageHandler(Locale.forLanguageTag("en-gb"));
        session = new SessionManager();

        PopupHandler threadsPopup = new PopupHandler(lang.get("question.THREADS"), "1024", "OK");
        threadsPopup.showAndWait();

        try {
            threads.set(Integer.parseInt(threadsPopup.responseText));
        } catch (NumberFormatException e) {
            logger.log(Level.SEVERE, "Invalid thread count.");
        }

        PopupHandler mongoDBURIPopup = new PopupHandler(lang.get("question.MONGO"), "mongodb://localhost:27017", "Done");
        mongoDBURIPopup.showAndWait();

        uri.set(mongoDBURIPopup.responseText);

        DatabaseHandler databaseHandler = new DatabaseHandler(uri.get());

        logger.log(Level.INFO, "Scanning IPs...");

        JFrame frame = new JFrame(lang.get("text.TITLE"));
        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        frame.setSize(300, 100);
        frame.setLayout(new BorderLayout());

        String currIPString = lang.get("text.CURRIP");
        JLabel statusLabel = new JLabel(currIPString.formatted(0));
        statusLabel.setHorizontalAlignment(0);
        frame.add(statusLabel, "Center");

        String foundString = lang.get("text.FOUND");
        JLabel foundLabel = new JLabel(foundString.formatted(0));
        foundLabel.setHorizontalAlignment(0);

        frame.add(foundLabel, "South");

        ServerList serverList = new ServerList(databaseHandler, lang);

        JButton viewServersButton = new JButton(lang.get("button.SERVERLIST"));

        viewServersButton.addActionListener(e -> {
            serverList.toggleGUI();
        });

        frame.add(viewServersButton, "North");

        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                stopping = true;

                SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
                    @Override
                    public Void doInBackground() {
                        serverList.hideGUI();
                        viewServersButton.setEnabled(false);
                        statusLabel.setText(lang.get("text.QUIT").formatted(ip));

                        logger.log(Level.INFO, "Stopping threads...");
                        try {
                            executor.shutdown();
                            var result = executor.awaitTermination(1, TimeUnit.MINUTES);
                        } catch (InterruptedException e) {
                            // pass
                        }
                        logger.log(Level.INFO, "Making an '.mcscanner' file...");
                        try {
                            BufferedWriter writer = new BufferedWriter(new FileWriter(".mcscanner"));
                            writer.write(offsetI + "\n" + offsetJ + "\n" + offsetK + "\n" + offsetL);
                            writer.close();
                        } catch (IOException e) {
                            logger.log(Level.SEVERE, "Failed to write '.mcscanner'!");
                        }
                        logger.log(Level.INFO, "Exiting...");

                        frame.dispose();
                        System.exit(0);
                        return null;
                    }
                };
                worker.execute();
            }
        });

        frame.setVisible(true);

        File offsetFile = new File(".mcscanner");
        if (offsetFile.exists()) {
            try {
                logger.log(Level.INFO, "Found '.mcscanner'.");
                BufferedReader reader = new BufferedReader(new FileReader(offsetFile));
                offsetI = Integer.parseInt(reader.readLine());
                offsetJ = Integer.parseInt(reader.readLine());
                offsetK = Integer.parseInt(reader.readLine());
                offsetL = Integer.parseInt(reader.readLine());
                reader.close();
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Failed to read '.mcscanner'.");
            }
            logger.log(Level.INFO, "Continuing from " + offsetI + "." + offsetJ + "." + offsetK + "." + offsetL + "...");
        }

        // TODO: Clean up this code please!
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
                            ip = i + "." + j + "." + k + "." + l;

                            while(executor.getQueue().size() >= (threads.get()*2)) {
                                // wait cuz if we submit too many threads to the pool's queue,
                                // it'll take a while to finish and people'll be dead by then!!!
                            }
                            ScannerThread scannerThread = new ScannerThread(ip, port, timeout, databaseHandler, session);
                            Thread scanThread = new Thread(scannerThread);
                            executor.submit(scanThread);

                            statusLabel.setText(currIPString.formatted(ip));
                            foundLabel.setText(foundString.formatted(session.foundThisSession));
                        }
                    }
                    thisOffsetL = 0;
                }
                thisOffsetK = 0;
            }
            thisOffsetJ = 0;
        }

        if(!stopping) {
            logger.log(Level.INFO, "Scan completed!");
            frame.dispose();
            System.exit(0);
        }
    }
}

class ScannerThread implements Runnable {
    private final String ip;
    private final int port;
    private final int timeout;
    private final DatabaseHandler dbHandler;
    private final SessionManager sessionManager;

    public ScannerThread(String ip, int port, int timeout, DatabaseHandler dbHandler, SessionManager sessionManager) {
        this.ip = ip;
        this.port = port;
        this.timeout = timeout;
        this.dbHandler = dbHandler;
        this.sessionManager = sessionManager;
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

            if (packetId != 0xFF) {
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

                if(dbHandler.isIPInDB(ip)) {
                    dbHandler.updateServerByIPInDB(ip, data[2], data[3], Integer.parseInt(data[5]));
                } else {
                    dbHandler.writeDetailsToDB(ip, data[2], data[3], Integer.parseInt(data[5]));
                }
            } else {
                data = string.split("§");

                if(dbHandler.isIPInDB(ip)) {
                    dbHandler.updateServerByIPInDB(ip, "1.6<=", data[0], Integer.parseInt(data[2]));
                } else {
                    dbHandler.writeDetailsToDB(ip, "1.6<=", data[0], Integer.parseInt(data[2]));
                }
            }

            socket.close();
            sessionManager.serverFound();
        } catch (IOException var8) {
            // No response/invalid response/timeout/port accidentally left open
        }
    }
}

class ServerList {
    private final DatabaseHandler dbHandler;
    private final LanguageHandler lang;
    private final JFrame frame;

    public ServerList(DatabaseHandler dbHandler, LanguageHandler lang) {
        this.lang = lang;
        this.dbHandler = dbHandler;
        this.frame = new JFrame(lang.get("text.SERVERLIST.TITLE").formatted(0));
        this.frame.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
        this.frame.setSize(720, 500);
        this.frame.setLayout(new BorderLayout());
        
        JTable table = new JTable();
        table.setModel(new DefaultTableModel(
            new Object[][] {
            },
            new String[] {
                this.lang.get("text.SERVERLIST.IP"), this.lang.get("text.SERVERLIST.MOTD"), this.lang.get("text.SERVERLIST.VERSION"), this.lang.get("text.SERVERLIST.MAX_PLAYERS")
            }
        ));
        table.getColumnModel().getColumn(0).setPreferredWidth(100);
        table.getColumnModel().getColumn(1).setPreferredWidth(200);
        table.getColumnModel().getColumn(2).setPreferredWidth(50);
        table.getColumnModel().getColumn(3).setPreferredWidth(50);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        table.setFillsViewportHeight(true);
        table.setRowHeight(20);
        table.setRowSelectionAllowed(true);
        table.setDefaultEditor(Object.class, null);
        table.setColumnSelectionAllowed(false);
        table.setCellSelectionEnabled(false);
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setResizingAllowed(false);
        
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        JTextField searchBar = new JTextField();
        searchBar.setHorizontalAlignment(0);
        searchBar.setToolTipText("Search");
        searchBar.addFocusListener(new PlaceholderText("Search", searchBar).getFocusAdapter());

        JComboBox<String> searchBy = new JComboBox<String>();
        searchBy.addItem(lang.get("dropdown.SERVERLIST.IP"));
        searchBy.addItem(lang.get("dropdown.SERVERLIST.MOTD"));
        searchBy.addItem(lang.get("dropdown.SERVERLIST.VERSION"));
        searchBy.addItem(lang.get("dropdown.SERVERLIST.MAX_PLAYERS"));
        searchBy.setSelectedIndex(2);

        searchBar.getDocument().addDocumentListener(new DocumentListener() {
            private void searchy() {
                String text = searchBar.getText();
                if(!text.isEmpty() && searchBar.hasFocus()) {
                    try {
                        ((TableRowSorter<TableModel>) table.getRowSorter()).setRowFilter(RowFilter.regexFilter(text, searchBy.getSelectedIndex()));
                    } catch (PatternSyntaxException pse) {
//                        JOptionPane.showMessageDialog(null, "Invalid search query/regex expression!", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                } else {
                    ((TableRowSorter<TableModel>) table.getRowSorter()).setRowFilter(null);
                }
            }
            @Override
            public void insertUpdate(DocumentEvent documentEvent) { searchy(); }

            @Override
            public void removeUpdate(DocumentEvent documentEvent) { searchy(); }

            @Override
            public void changedUpdate(DocumentEvent documentEvent) { searchy(); }
        });

        Timer timer = new Timer(3000, e -> {
            ((DefaultTableModel) table.getModel()).setRowCount(0);
            ArrayList < Document > documents1 = this.dbHandler.getServers();
            this.frame.setTitle(lang.get("text.SERVERLIST.TITLE").formatted(documents1.size()));
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
        timer.setRepeats(true);
        timer.start();
        timer.getListeners(ActionListener.class)[0].actionPerformed(null);

        TableRowSorter < TableModel > sorter = new TableRowSorter<> (table.getModel());
        table.setRowSorter(sorter);

        table.getSelectionModel().addListSelectionListener(event -> {
            try {
                if(!event.getValueIsAdjusting()) {
                    var selIP = table.getValueAt(table.getSelectedRow(), 0).toString();
                    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(selIP), null);
                    JOptionPane.showMessageDialog(null, "Copied IP: %s".formatted(selIP), "Copied", JOptionPane.INFORMATION_MESSAGE);
//                    table.changeSelection(0, 0, false, false);
                }
            } catch (Exception e) {
                // welp
            }
        });

        this.frame.add(scrollPane, BorderLayout.CENTER);
        this.frame.add(searchBy, BorderLayout.SOUTH);
        this.frame.add(searchBar, BorderLayout.NORTH);
    }

    public boolean toggleGUI() {
        this.frame.setVisible(!this.frame.isVisible());
        return this.frame.isVisible();
    }

    public boolean hideGUI() {
        this.frame.setVisible(false);
        return this.frame.isVisible();
    }
}
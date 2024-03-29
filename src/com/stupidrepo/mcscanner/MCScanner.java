package com.stupidrepo.mcscanner;

import com.stupidrepo.mcscanner.language.LanguageHandler;
import org.bson.Document;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
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

    public static void main(String[] args) {
        AtomicInteger threads = new AtomicInteger(1024);
        int timeout = 1300;
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

        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(threads.get());

        PopupHandler mongoDBURIPopup = new PopupHandler(lang.get("question.MONGO"), "mongodb://localhost:27017", "Done");
        mongoDBURIPopup.showAndWait();

        uri.set(mongoDBURIPopup.responseText);

        DatabaseHandler databaseHandler = new DatabaseHandler(uri.get());

        logger.log(Level.INFO, "Scanning IPs...");

        JFrame frame = new JFrame(lang.get("text.TITLE"));
        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        frame.setSize(350, 100);
        frame.setResizable(false);
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

        viewServersButton.addActionListener(e -> serverList.toggleGUI());

        frame.add(viewServersButton, "North");

        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                stopping = true;

                SwingWorker<Void, Void> worker = new SwingWorker<>() {
                    @Override
                    public Void doInBackground() {
                        serverList.hideGUI();
                        viewServersButton.setEnabled(false);

                        logger.log(Level.INFO, "Stopping threads...");
                        executor.shutdown();
                        try {
                            executor.awaitTermination(3, TimeUnit.MINUTES);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
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

        // TODO: Better text update code? Works fine right now.
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            public Void doInBackground() {
                databaseHandler.addListener((updated, row) -> {
                    if(!updated) session.serverFound();
                });
                while (true) {
                    if (stopping) {
                        statusLabel.setText(lang.get("text.QUIT").formatted(executor.getQueue().size() + executor.getActiveCount()));
                        foundLabel.setText(foundString.formatted(session.foundThisSession));
                    } else {
                        statusLabel.setText(currIPString.formatted(ip));
                        foundLabel.setText(foundString.formatted(session.foundThisSession));
                    }
                }
            }
        };
        worker.execute();

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

                            while(executor.getQueue().size() >= (threads.get()*2));
                            ScannerThread scannerThread = new ScannerThread(ip, port, timeout, databaseHandler, session);
                            Thread scanThread = new Thread(scannerThread);
                            executor.submit(scanThread);
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

    public ScannerThread(String ip, int port, int timeout, DatabaseHandler dbHandler, SessionManager sessionManager) {
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
                    (byte) 0xFE,
                    (byte) 0x01
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
                var motd = data[3].replaceAll("§[0-9a-fk-or]", "");;

                if(dbHandler.isIPInDB(ip)) {
                    dbHandler.updateServerByIPInDB(ip, data[2], motd, Integer.parseInt(data[4]), Integer.parseInt(data[5]));
                } else {
                    dbHandler.writeDetailsToDB(ip, data[2], motd, Integer.parseInt(data[4]), Integer.parseInt(data[5]));
                }
            } else {
                data = string.split("§");

                if(dbHandler.isIPInDB(ip)) {
                    dbHandler.updateServerByIPInDB(ip, "1.6<=", data[0], Integer.parseInt(data[1]), Integer.parseInt(data[2]));
                } else {
                    dbHandler.writeDetailsToDB(ip, "1.6<=", data[0], Integer.parseInt(data[1]), Integer.parseInt(data[2]));
                }
            }

            socket.close();
        } catch (IOException var8) {
            // No response/invalid response/timeout/port accidentally left open
        }
    }
}

class ServerList {
    private final DatabaseHandler dbHandler;
    private final JFrame frame;

    private Object getDataUnlessNull(Document document, String key, Object ifNull) {
        if(document.get(key) != null) {
            return document.get(key);
        } else {
            return ifNull;
        }
    }

    public ServerList(DatabaseHandler dbHandler, LanguageHandler lang) {

        this.dbHandler = dbHandler;
        this.frame = new JFrame(lang.get("text.SERVERLIST.TITLE").formatted(0));
        this.frame.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
        this.frame.setSize(850, 500);
        this.frame.setResizable(false);
        this.frame.setLayout(new BorderLayout());
        
        JTable table = new JTable();
        table.setAutoCreateRowSorter(false);
        table.setRowSorter(null);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        table.setFillsViewportHeight(true);
        table.setRowHeight(20);
        table.setRowSelectionAllowed(true);
        table.setDefaultEditor(Object.class, null);
        table.setColumnSelectionAllowed(false);
        table.setCellSelectionEnabled(false);
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setResizingAllowed(false);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value,
                        isSelected, hasFocus, row, column);
                // align text to center
                setHorizontalAlignment(JLabel.CENTER);
                setToolTipText(value.toString());
                return c;
            }
        });

        table.setModel(new DefaultTableModel(
            new Object[][] {
            },
            new String[] {
                lang.get("text.SERVERLIST.IP"),
                    lang.get("text.SERVERLIST.MOTD"),
                    lang.get("text.SERVERLIST.VERSION"),
                    lang.get("text.SERVERLIST.PLAYERS"),
                    lang.get("text.SERVERLIST.LAST_UPDATED")
            }
        ));
        table.getColumnModel().getColumn(0).setPreferredWidth(100);
        table.getColumnModel().getColumn(1).setPreferredWidth(250);
        table.getColumnModel().getColumn(2).setPreferredWidth(50);
        table.getColumnModel().getColumn(3).setPreferredWidth(150);
        table.getColumnModel().getColumn(4).setPreferredWidth(200);
        
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        JTextField searchBar = new JTextField();
        searchBar.setHorizontalAlignment(0);
        searchBar.setToolTipText("Search");
        searchBar.addFocusListener(new PlaceholderText("Search", searchBar).getFocusAdapter());

        JComboBox<String> searchBy = new JComboBox<>();
        searchBy.addItem(lang.get("dropdown.SERVERLIST.IP"));
        searchBy.addItem(lang.get("dropdown.SERVERLIST.MOTD"));
        searchBy.addItem(lang.get("dropdown.SERVERLIST.VERSION"));
        searchBy.addItem(lang.get("dropdown.SERVERLIST.PLAYERS"));
        searchBy.addItem(lang.get("dropdown.SERVERLIST.LAST_UPDATED"));
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

        // TODO: Use events and DatabaseHandler for this
        ArrayList < Document > documents1 = this.dbHandler.getServers();
        frame.setTitle(lang.get("text.SERVERLIST.TITLE").formatted(documents1.size()));

        for (Document document: documents1) {
            if (document == null || document.isEmpty() || document.get("ip") == null) {
                continue;
            }
            var ip = getDataUnlessNull(document, "ip", "[error]").toString();
            var motd = getDataUnlessNull(document, "motd", "Seems like there is a corrupted server entry!").toString();
            var version = getDataUnlessNull(document, "version", "=/").toString();
            var players = Integer.parseInt(getDataUnlessNull(document, "currentPlayers", -1).toString());
            var maxPlayers = Integer.parseInt(getDataUnlessNull(document, "maxPlayers", -1).toString());
            var lastUpdated = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(getDataUnlessNull(document, "lastUpdated", new Date(0)));

            ((DefaultTableModel) table.getModel()).addRow(new Object[]{
                    ip, motd, version, "%d/%d".formatted(players, maxPlayers), lastUpdated
            });
        }

        dbHandler.addListener((updated, row) -> {
            if(updated) {
                for(int i = 0; i < table.getRowCount(); i++) {
                    if(table.getValueAt(i, 0).equals(row[0])) {
                        ((DefaultTableModel) table.getModel()).removeRow(i);
                        ((DefaultTableModel) table.getModel()).insertRow(i, row);
                        return;
                    }
                }
                // if it gets here, it's a new server
                ((DefaultTableModel) table.getModel()).addRow(row);
            } else {
                frame.setTitle(lang.get("text.SERVERLIST.TITLE").formatted(dbHandler.getServers().size()));
                ((DefaultTableModel) table.getModel()).addRow(row);
            }
        });

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

    public void toggleGUI() {
        this.frame.setVisible(!this.frame.isVisible());
    }

    public void hideGUI() {
        this.frame.setVisible(false);
    }
}
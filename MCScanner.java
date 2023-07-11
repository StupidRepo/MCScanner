import java.awt.BorderLayout;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Iterator;
import javax.swing.JFrame;
import javax.swing.JLabel;
// import javax.swing.JProgressBar;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

public class MCScanner {
   public static void main(String[] var0) {
    int threads = 1024;
    int timeout = 1000;
    int minimumRange = 1;
    int maxRange = 255;
    int port = 25565;

    double progressThing = (maxRange - minimumRange + 1) * 256 * 256;

    String filename = "output.txt";

    float version = 1.08f;

    ArrayList<Thread> threadList = new ArrayList<Thread>();

    System.out.println("MCScanner v" + version + "!");
    System.out.println("Starting scan...");

    JFrame frame = new JFrame("MCScanner v" + version);
    frame.setDefaultCloseOperation(3);
    frame.setSize(300, 100);
    frame.setLayout(new BorderLayout());

    // JProgressBar progressBar = new JProgressBar(0, (int)progressThing);
    // progressBar.setStringPainted(true);

    // frame.add(progressBar, "Center");

    JLabel scannedLabel = new JLabel("Scanned: 0/" + progressThing*256);
    scannedLabel.setHorizontalAlignment(0);

    // frame.add(scannedLabel, "South");

    frame.add(scannedLabel, "Center");
    frame.setVisible(true);

    long scanned = 0;

    for(int i = minimumRange; i <= maxRange; ++i) {
        for(int j = 0; j <= 255; ++j) {
            for(int k = 0; k <= 255; ++k) {
                    for (int l = 0; l <= 255; ++l) {
                    // String ip = "" + i + "." + j + "." + k + ".0";
                    String ip = i + "." + j + "." + k + "." + l;

                    Thread scanThread = new Thread(new ScannerThread(ip, port, timeout, filename));
                    threadList.add(scanThread);
                    scanThread.start();

                    if (threadList.size() >= threads) {
                        Iterator iterator = threadList.iterator();

                        while(iterator.hasNext()) {
                            Thread nextThread = (Thread)iterator.next();

                            try {
                                nextThread.join();
                                ++scanned;
                                // progressBar.setValue(scanned);
                                scannedLabel.setText("Scanned: " + scanned + "/" + progressThing*256 + " (" + Math.round((scanned / progressThing*256)*100)/100 + "%)");
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

    Iterator yetAnotherIterator = threadList.iterator();

    while(yetAnotherIterator.hasNext()) {
        Thread nextThreadAgain = (Thread)yetAnotherIterator.next();

        try {
            nextThreadAgain.join();
            ++scanned;
            // progressBar.setValue(scanned);
            scannedLabel.setText("Scanned: " + scanned + "/" + progressThing*256);
        } catch (InterruptedException timeout1) {
            // well
        }
    }

    frame.setVisible(false);
    frame.dispatchEvent(new WindowEvent(frame, 201));
    System.out.println("Scan completed!");
    }
}

class ScannerThread implements Runnable {
   private String ip;
   private int port;
   private int timeout;
   private String filename;

   public ScannerThread(String ip, int port, int timeout, String filename) {
      this.ip = ip;
      this.port = port;
      this.timeout = timeout;
      this.filename = filename;
   }

   public void run() {
        try {
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress(this.ip, this.port), this.timeout);

            InputStream inp = socket.getInputStream();
            OutputStream outp = socket.getOutputStream();
            
            outp.write(new byte[]{-2, 1});

            socket.setSoTimeout(this.timeout);

            byte[] bytesVar = new byte[256];
            int bytesResp = inp.read(bytesVar);

            if (bytesResp > 0) {
                System.out.println("Found a server!");
                String var6 = this.ip + "\n";
                FileWriter var7 = new FileWriter(this.filename, true);
                var7.write(var6);
                var7.close();
            }

            socket.close();
        } catch (IOException var8) {
            // well
        }
    }
}

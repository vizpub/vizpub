package no.uio.ifi.vizpub.collector;

import com.esotericsoftware.minlog.Log;
import no.uio.ifi.vizpub.utils.LogCategory;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Multithreaded server class responsible for collecting data from the reporter. Spawns a
 * new thread with a <code>CollectorWorker</code> runnable for each connection.
 *
 * @author Nils Peder Korsveien
 * @see CollectorWorker
 * @see no.uio.ifi.vizpub.reporter.ReporterService
 * @see no.uio.ifi.vizpub.reporter.Reporter
 */
public class Collector implements Runnable {
    private int serverPort;

    public Collector(String filePath) {
        loadProperties(filePath);

    }

    private void loadProperties(String filePath) {
        Properties properties = new Properties();
        try {
            properties.load(new FileInputStream(filePath));
            serverPort = Integer.parseInt(properties.getProperty("port"));
            Log.info(LogCategory.COLLECTOR, "listening on port " + serverPort);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void run() {

//        if (Engine.getType() == Engine.Type.NET) {
//            Socket clientSocket;
//            try (ServerSocket serverSocket = new ServerSocket(this.serverPort)
//            ) {
//                serverSocket.setReuseAddress(true);
//                for (; ; ) {
//                    clientSocket = serverSocket.accept();
//                    new Thread(
//                            new CollectorWorker(clientSocket)
//                    ).start();
//                }
//            } catch (IOException e) {
//                throw new RuntimeException(
//                        "Error accepting client connection", e);
//            }
//        } else if (Engine.getType() == Engine.Type.SIM) {
            Log.info(LogCategory.COLLECTOR, "Running in SIM mode, reports are handled by the reporter");
//        }
    }
}

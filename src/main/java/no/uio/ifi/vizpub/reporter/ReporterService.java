package no.uio.ifi.vizpub.reporter;

import com.esotericsoftware.minlog.Log;
import com.google.common.collect.Multimap;
import com.google.common.util.concurrent.AbstractExecutionThreadService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import no.uio.ifi.vizpub.collector.CollectorWorker;
import no.uio.ifi.vizpub.reports.Report;
import no.uio.ifi.vizpub.utils.LogCategory;
import no.uio.ifi.vizpub.utils.gson.MultimapDeserializer;

import java.io.FileInputStream;
import java.util.Properties;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Service responsible for creating a report of the current state of the system.
 *
 * @author Nils Peder Korsveien
 */
public class ReporterService extends AbstractExecutionThreadService {
    private int port;
    private String host;
    private static BlockingQueue<Report> reportQueue;
    private final Report POISON_PILL = new Report();

    public ReporterService(String filePath) {
        reportQueue = new ArrayBlockingQueue<>(100);
        loadProperties(filePath);
    }

    @Override
    public void run() {

//        if (Engine.getType() == Engine.Type.NET) {
//            try (Socket clientSocket = new Socket(host, port);
//                 ObjectOutputStream objectOutputStream = new ObjectOutputStream((clientSocket.getOutputStream()))) {
//
//                for (; ; ) {
//                    Report overlay = reportQueue.take();
//                    Log.debug(LogCategory.GEXF, "On consumer side of queue: " + overlay.getNodes().size());
//                    Gson gson = new GsonBuilder().registerTypeAdapter(Multimap.class, new MultimapSerializer()).create();
//                    String json = gson.toJson(overlay);
//
//                    if (overlay == POISON_PILL) {
//                        clientSocket.shutdownOutput();
//                        break;
//                    }
//                    objectOutputStream.writeObject(json);
//
//                    Log.info(LogCategory.REPORTER, "Sent report for protocol "
//                            + overlay.getProtocolName()
//                            + " to "
//                            + host
//                            + " on port "
//                            + port);
//
//                    objectOutputStream.flush();
//
//                    // give Collector time to reenter accept()
//                    Thread.sleep(500);
//                }
//
//            } catch (IOException | InterruptedException e) {
//                e.printStackTrace();
//            }
//        } else if (Engine.getType() == Engine.Type.SIM) {

            String protocolName = null;
            for (; ; ) {

                Report report = null;
                try {
                    report = reportQueue.take();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (report == POISON_PILL) {
                    break;
                }
                checkNotNull(report, "Failed to retrieve report");

                Gson gson = new GsonBuilder().registerTypeAdapter(Multimap.class, new MultimapDeserializer()).create();
                String json = gson.toJson(report);
                protocolName = report.getProtocolName();

                if (protocolName == null) {
                    Log.error(LogCategory.COLLECTOR, "Protocol name is missing from the reports");
                    return;
                }

                Log.info(LogCategory.REPORTER, "Writing report to file...");
                CollectorWorker.saveJsonReport(json, host, protocolName);
            }
        CollectorWorker.finish(protocolName);
    }

    @Override
    public void triggerShutdown() {
        try {
            reportQueue.put(POISON_PILL);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static BlockingQueue<Report> getReportQueue() {
        return reportQueue;
    }

    /**
     * Private methods:
     */

    private void loadProperties(String filePath) {
        Properties properties = new Properties();
        try {
            properties.load(new FileInputStream(filePath));
            host = properties.getProperty("host");
            port = Integer.parseInt(properties.getProperty("port"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

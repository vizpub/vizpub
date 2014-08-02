package no.uio.ifi.vizpub.collector;

import com.esotericsoftware.minlog.Log;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Multimap;
import com.google.common.collect.Table;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import no.uio.ifi.vizpub.collector.gexf.GexfBuilder;
import no.uio.ifi.vizpub.reports.Report;
import no.uio.ifi.vizpub.utils.LogCategory;
import no.uio.ifi.vizpub.utils.gson.MultimapDeserializer;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.net.Socket;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * For every connection from the reporter, the <code>Collector</code> initiates a new thread of this class. This
 * class will be responsible for collecting reports from its associated reporter, and will exit from its
 * <code>run</code> method when the client exits. As the <code>run</code> method exits, it will decrement a counter
 * found in the <code>CollectorPostProcessor</code> class. When the counter reaches zero, the collector will start
 * the offline processing of data collected from the reporters and output a single .gexf file.
 *
 * @author Nils Peder Korsveien
 * @see Collector
 * @see no.uio.ifi.vizpub.collector.gexf.GexfBuilder
 */
public class CollectorWorker implements Runnable {
    public static final String REPORT_UNPROCESSED = "reports/unprocessed/";
    public static final String REPORT_PROCESSED = "reports/processed/";

    private final Socket socket;
    private static AtomicInteger collectorWorkerCount = new AtomicInteger();
    private static Table<String, String, Integer> reportingIntervalCounter = HashBasedTable.create();

    public CollectorWorker(Socket socket) {
        this.socket = socket;
        collectorWorkerCount.incrementAndGet();
    }

    public static int numberOfInstances() {
        return collectorWorkerCount.get();
    }

    public void run() {
        String protocolName = null;
        try (ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream())) {
            Log.debug(LogCategory.COLLECTOR, "Reporter connected from " + socket.getInetAddress().getHostName());

            String json;
            while ((json = (String) inputStream.readObject()) != null) {
                Log.info(LogCategory.COLLECTOR, "Saving report from " + socket.getInetAddress().getHostName());

                String hostName = socket.getInetAddress().getHostName();

                Gson gson = new GsonBuilder().registerTypeAdapter(Multimap.class, new MultimapDeserializer()).create();
                Report report = gson.fromJson(json, Report.class);
                protocolName = report.getProtocolName();
                saveJsonReport(json, hostName, protocolName);

                json = null;
            }
        } catch (EOFException eofe) {
            Log.debug(LogCategory.COLLECTOR,
                    socket.getInetAddress().getHostName()
                            + " disconnected!"
            );

            if (collectorWorkerCount.decrementAndGet() == 0) {
                Log.info(LogCategory.COLLECTOR, "All CollectorWorker threads have finished...");
                finish(protocolName);
                Log.info(LogCategory.COLLECTOR, "Exiting...");
                System.exit(0);
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static void finish(String protocolName) {
        checkNotNull(protocolName, "Protocol name not found in report");
        Log.info(LogCategory.COLLECTOR, "Finishing...");

//        if (new File(REPORT_UNPROCESSED).exists()) {
//            Log.info(LogCategory.COLLECTOR, "Unprocessed dir found...");
//            appendJsonReportsInDirectory(REPORT_UNPROCESSED, protocolName);
//            try {
//                FileUtils.forceDelete(new File(REPORT_UNPROCESSED));
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }

        if (new File(REPORT_PROCESSED).exists()) {
            Log.info(LogCategory.COLLECTOR, "Processed dir found...");
            GexfBuilder gexfBuilder = new GexfBuilder();
            gexfBuilder.createStructuralOverlay(REPORT_PROCESSED, protocolName);
            gexfBuilder.createDisseminationOverlay(REPORT_PROCESSED, protocolName, "0");
        }

//        if (Engine.getType() == Engine.Type.SIM) {
            System.exit(0);
//        }
    }

    public static void saveJsonReport(String json, String hostName, String protocolName) {
        checkNotNull(json);
        checkNotNull(hostName);
        checkNotNull(protocolName);

        String checkPointNumber = createSubDir(protocolName, hostName);

        // write report to json file
        final String pathname = REPORT_PROCESSED
                + protocolName
                + "/"
                + checkPointNumber
                + "_"
                + hostName
                + "_"
                + protocolName
                + ".json";

//        // write report to json file
//        final String pathname = REPORT_UNPROCESSED
//                + protocolName
//                + "/"
//                + checkPointNumber
//                + "/"
//                + checkPointNumber
//                + "_"
//                + hostName
//                + "_"
//                + protocolName
//                + ".json";

        try {
            FileUtils.writeStringToFile(new File(pathname), json);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void appendJsonReportsInDirectory(String directory, String protocolName) {
        checkNotNull(directory);
        checkNotNull(protocolName);

        // traverse top directory
        Path topDir = FileSystems.getDefault().getPath(directory + protocolName);
        int dirCount = 0;
        Gson gson ;
        Report.Builder overlayBuilder = Report.builder();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(topDir)) {
            for (Path subDir : stream) {
                Log.debug(LogCategory.GEXF, "subDir: " + subDir);

                try ( DirectoryStream<Path> subDirStream = Files.newDirectoryStream(subDir)) {
                    checkNotNull(subDirStream);

                    boolean first = true;
                    gson = new GsonBuilder().registerTypeAdapter(Multimap.class, new MultimapDeserializer()).create();

                    // iterate through files in sub directory
                    for (Path file : subDirStream) {
                        Log.debug(LogCategory.GEXF, "file: " + file);

                        try (FileInputStream fileInputStream = new FileInputStream(file.toFile())) {
                            Reader reader = new InputStreamReader(fileInputStream);
                            Report overlay = gson.fromJson(reader, Report.class);

                            if (overlay == null) {
                                Log.info(LogCategory.GEXF, "Json report is empty, returning...");
                                return;
                            }

                            Log.debug(LogCategory.GEXF, "Appending node count: " + overlay.getNodes().size());

                            if (first) {
                                overlayBuilder
                                        .withProtocolId(overlay.getProtocolId())
                                        .withProtocolName(overlay.getProtocolName());
                                first = false;
                            }

                            // add nodes and edges
                            overlayBuilder.addNodes(overlay.getNodes());
                            overlayBuilder.addEdges(overlay.getEdges());
                            overlayBuilder.addPublications(overlay.getPublications());

                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
                // write appended reports to json file
                String filePrefix = String.format("%08d", ++dirCount);
                String fileName = CollectorWorker.REPORT_PROCESSED + protocolName + "/" + filePrefix + protocolName + ".json";
                String appendedJson = gson.toJson(overlayBuilder.build());

                Log.debug(LogCategory.GEXF, "Writing to file: " + fileName);
                try {
                    FileUtils.writeStringToFile(new File(fileName), appendedJson);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }


    private static String createSubDir(String protocolName, String hostName) {
        String checkpointNumber = getCheckpointNumber(protocolName, hostName);
        try {
            FileUtils.forceMkdir(new File(CollectorWorker.REPORT_UNPROCESSED
                    + protocolName
                    + "/"
                    + checkpointNumber));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return checkpointNumber;
    }

    /**
     * Returns the correct checkpoint number for the current report
     *
     * @param protocolName name of protocol being reported
     * @param hostName     name of host that delivered the report
     * @return the correct checkpoint number
     */
    private static String getCheckpointNumber(String protocolName, String hostName) {
        if (!reportingIntervalCounter.contains(protocolName, hostName)) {
            reportingIntervalCounter.put(protocolName, hostName, 0);
        } else {
            int counter = reportingIntervalCounter.get(protocolName, hostName);
            reportingIntervalCounter.put(protocolName, hostName, ++counter);
        }
        Integer fileNumber = reportingIntervalCounter.get(protocolName, hostName);
        return String.format("%08d", fileNumber);
    }

    public static void main(String[] args) {
        Log.DEBUG();
//        finish(args[0]);
        finish("protocol.rings");
    }
}

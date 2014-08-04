package no.uio.ifi.vizpub.reporter;

import com.esotericsoftware.minlog.Log;
import com.google.common.base.Stopwatch;
import no.uio.ifi.vizpub.Reportable;
import no.uio.ifi.vizpub.reports.NodeData;
import no.uio.ifi.vizpub.reports.PubMessage;
import no.uio.ifi.vizpub.reports.Report;
import no.uio.ifi.vizpub.utils.LogCategory;
import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Control;
import peersim.core.Network;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Peernet controller responsible for initating the collection and sending of a report by calling the
 * <code>ReporterService#report()</code> method. This controller should be configured
 * via the PeerNet configuration file. The following example configuration will execute
 * the observer each 10 seconds, reporting the state of a protocol with the name cyclon.
 * Note that the observer must be included as a control, otherwise it will be ignored by
 * the simulator.
 * <p/>
 * <pre>
 *     {@code
 *     include.control    reporter
 *
 *     control.reporter        peernet.vizpub.reporter.Reporter
 *     {
 *          step                10000
 *          protocol            cyclon
 *     }
 *     }
 * </pre>
 *
 * @author Nils Peder Korsveien
 * @see no.uio.ifi.vizpub.reporter.ReporterService
 * @see no.uio.ifi.vizpub.collector.Collector
 * @see no.uio.ifi.vizpub.reports.NodeData
 * @see no.uio.ifi.vizpub.reports.EdgeData
 * @see no.uio.ifi.vizpub.reports.PubMessage
 */
public class Reporter implements Control {
    private static final String PAR_PROTOCOL = "protocol";
    protected int protocolId;
    protected static int reporterIntervalCount;

//    protected PublicationSender publicationSender;
//    protected ChurnInjector churnInjector;

    private static String PAR_PUBLICATION_SENDER = "publicationsender";
    private static String PAR_CHURN_INJECTOR = "churninjector";

    private static String PAR_CHURN_ENABLED = "churn_enabled";
    private static String PAR_PUBLICATIONS_ENABLED = "publications_enabled";

//    private final boolean churnEnabled;
//    private final boolean publicationsEnabled;

    public Reporter(String name) {
        protocolId = Configuration.getPid(name + "." + PAR_PROTOCOL);
        reporterIntervalCount = 0;

//        if (Configuration.getString(name + "." + PAR_PROTOCOL).equals("scribe")) {
//            trafficGenerator = new RCTrafficGenerator(name);
//        } else {
//            publicationSender = new PublicationSender(name + "." + PAR_PUBLICATION_SENDER);
//        }

//        churnInjector = new ChurnInjectorImpl(name + "." + PAR_CHURN_INJECTOR);

//        churnEnabled = Configuration.getBoolean(name + "." + PAR_CHURN_ENABLED);
//        publicationsEnabled = Configuration.getBoolean(name + "." + PAR_PUBLICATIONS_ENABLED);
    }

    @Override
    public boolean execute() {
        Stopwatch stopwatch = Stopwatch.createStarted();
//        if (Engine.getType() == Engine.Type.NET && CommonState.finishedBootstrapping) {
//
//        }

//        if (ChurnInjector.hasExecuted()) {
//            report(protocolId);
//        }

//        if (churnEnabled) {
//            churnInjector.execute();
//        } else {
            report(protocolId);
//        }

        // when running poldercast experiments
//        if (publicationSender != null && publicationsEnabled) {
//            publicationSender.execute();
//        }

//        // when running scribe experiments
//        if (trafficGenerator != null && publicationsEnabled) {
//            trafficGenerator.execute();
//        }

        Log.info(LogCategory.REPORTER, "Report no. : " + reporterIntervalCount);
        reporterIntervalCount++;
        return false;
    }

    protected void report(int protocolId) {
        try {
            final String[] protocolNames = Configuration.getNames("protocol");
            Report overlay = createReport(protocolNames[protocolId], protocolId);
            ReporterService.getReportQueue().put(overlay);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Creates a report based on data collected through the reportable interface.
     * <p/>
     * This method will create a report describing the state of the overlay for the
     * corresponding PeerNet instance. What properties to report can be adjusted by
     * taking advantage to the <class>NodeData</class> and <class>EdgeData</class> builder
     * pattern:
     * {@code
     * NodeData node = NodeData.builder()
     * .withId(reportable.reportId())
     * .withNeighbors(reportable.reportNeighborIds())
     * .withTopics(reportable.reportTopics())
     * .build();
     * }
     * <p/>
     * The code sample above will report the id of the node, as well as the id of the neighbors (required in order for
     * the collector to rebuild the structural overlay) and the topics the node subscribes to.
     *
     * @param protocolName
     * @param protocolId
     * @return
     */
    protected Report createReport(String protocolName, int protocolId) {
        Map<String, NodeData> nodes = new HashMap<>();
        Map<String, PubMessage> publications = new HashMap<>();

        for (int i = 0; i < Network.size(); i++) {


            if (!Network.get(i).isUp()) {
                continue;
            }

            Reportable reportable = (Reportable) Network.get(i).getProtocol(protocolId);

            // create nodes
            //TODO: should read settings from config
            NodeData node = NodeData.builder()
                    .withId(reportable.reportId())
                    .withNeighbors(reportable.reportNeighborIds())
//                    .withTopics(reportable.reportTopics())
//                    .withControlMsgsSent(reportable.reportControlMsgsSent())
//                    .withControlMsgsReceived(reportable.reportControlMsgsReceived())
//                    .withBitsSent(reportable.reportControlBytesSent())
//                    .withBitsReceived(reportable.reportControlBytesReceived())
//                    .withSubscriptionSize(reportable.reportSubscriptionSize())
//                    .withPublicationMsgsSent(reportable.reportPubMsgsSent())
//                    .withPublicationMsgsReceived(reportable.reportPubMsgsReceived())
//                    .withDuplicateMsgs(reportable.reportDuplicatePubMsgs())
                    .build();

            nodes.put(node.getId(), node);
            Log.debug(LogCategory.REPORTER, "Network size: " + nodes.size());

            //TODO: should be determined by config
//            for (PubMessage pubMessage : node.getPublicationMsgsSent().values()) {
//                if (!publications.containsKey(pubMessage.getMsgId())) {
//                    publications.put(pubMessage.getMsgId(), pubMessage);
//                }
//            }
//            reportable.clearPublications();
        }

//        Log.debug(LogCategory.REPORTER, "Added " + publications.size() + " publications to report");
//        Log.debug(LogCategory.REPORTER, "Overlay size: " + nodes.size());

        return Report.builder()
                .withProtocolId(protocolId)
                .withProtocolName(protocolName)
                .withIntervalCount(reporterIntervalCount)
                .withNodes(nodes)
//                .withPublications(publications)
                .build();
    }
}

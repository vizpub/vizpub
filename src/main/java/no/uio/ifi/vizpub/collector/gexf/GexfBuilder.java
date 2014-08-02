package no.uio.ifi.vizpub.collector.gexf;

import com.esotericsoftware.minlog.Log;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import it.uniroma1.dis.wsngroup.gexf4j.core.*;
import it.uniroma1.dis.wsngroup.gexf4j.core.dynamic.Spell;
import it.uniroma1.dis.wsngroup.gexf4j.core.dynamic.TimeFormat;
import it.uniroma1.dis.wsngroup.gexf4j.core.impl.GexfImpl;
import it.uniroma1.dis.wsngroup.gexf4j.core.impl.SpellImpl;
import it.uniroma1.dis.wsngroup.gexf4j.core.impl.StaxGraphWriter;
import no.uio.ifi.vizpub.collector.CollectorWorker;
import no.uio.ifi.vizpub.reports.EdgeData;
import no.uio.ifi.vizpub.reports.NodeData;
import no.uio.ifi.vizpub.reports.PubMessage;
import no.uio.ifi.vizpub.reports.Report;
import no.uio.ifi.vizpub.utils.LogCategory;
import no.uio.ifi.vizpub.utils.gson.MultimapDeserializer;
import org.apache.commons.io.FileUtils;
import org.joda.time.DateTime;

import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Class handling the offline processing of reports collected from the reporter.
 * <p/>
 * This class is responsible for reading and processing all data collected from the reporter. This includes
 * both appending reports collected from separate sites into one single report describing the entire system
 * state at a specific reporting interval, as well as creating a single dynamic gexf file based on these derived reports.
 * <p/>
 * This class can build two types of gexf overlays: structural overlays and dissemination overlays.
 * <p/>
 * Structural overlays are built by collecting the id of each node as well as the id of their neighbors. Structural
 * metrics can be added through node and edge attributes. Which metrics to include can be chosen by using the
 * <class>NodeAttributes</class> and <class>EdgeAttributes</class>, who both conform to the builder pattern:
 * <p/>
 * {@code
 * nodeAttributes = NodeAttributes.builder()
 * .withTopics()
 * .withSubscriptionSize()
 * .withPublicationsSent()
 * .withPublicationsReceived()
 * .withHopCount()
 * .withDuplicateCount()
 * .build();
 * <p/>
 * edgeAttributes = EdgeAttributes.builder()
 * .withPublications()
 * .build();
 * }
 * <p/>
 * Note that for some of the normalized metrics such as average control messages sent, it is necessary to call the
 * <code>calculateAverages</code> method which utilizes the <class>GephiProcessing</class> class.
 * <p/>
 * Dissemination overlays are constructed by analyzing which publication messages each node sent and received. Each edge
 * will have a publications attribute enabling filtering out dissemination graphs for each publication sent, enabling
 * calculating metrics such as average path length. The dissemination edges are also added wrt hop count in order to
 * enable step-by-step playback of a publication message dissemination. When building dissemination graphs, metrics such
 * as hit ratio can be included using the <class>GraphAttributes</class> class:
 * <p/>
 * {@code
 * graphAttributes = GraphAttributes.builder(graph)
 * .withHitRatioPerTopic()
 * .withHitRatioPerPublication()
 * .build();}
 *
 * @author Nils Peder Korsveien
 */
public class GexfBuilder {

    private Map<String, Node> aliveNodes;
    private Map<String, Edge> aliveEdges;
    private Map<String, Node> deadNodes;
    private Map<String, Edge> deadEdges;
    private Map<String, Spell> nodeSpells;
    private Map<String, Spell> edgeSpells;
    private NodeAttributes nodeAttributes;
    private EdgeAttributes edgeAttributes;
    private Graph graph;
    private it.uniroma1.dis.wsngroup.gexf4j.core.Gexf gexf;

    private Map<String, EdgeData> edges;

    private double animationLength;

    /**
     * Key:   hop count + reporting interval
     * Value: the dissemination edge
     */
    Multimap<Double, EdgeData> disseminationEdges;

    public GexfBuilder() {
        aliveNodes = new HashMap<>();
        aliveEdges = new HashMap<>();
        deadNodes = new HashMap<>();
        deadEdges = new HashMap<>();
        nodeSpells = new HashMap<>();
        edgeSpells = new HashMap<>();
        edges = new HashMap<>();
        animationLength = 0;
    }

    /**
     * Creates a gexf dissemination overlay for publication with given id based on data collected from reporter.
     * <p/>
     * This overlay will trace a single publication by building edges according to hop count.
     * This enables step by step playback in Gephi.
     *
     * @param directory    directory of reports
     * @param protocolName name of protocol described by reports
     * @param messageId    id of message to trace
     */
    public void createDisseminationOverlay(String directory, String protocolName, String messageId) {
        Log.info(LogCategory.GEXF, "Creating dissemination graph");

        clearCaches();

        gexf = new GexfImpl();
        gexf.getMetadata()
                .setCreator("VizPub")
                .setDescription("Dissemination Overlay for publication message " + messageId)
                .setLastModified(Calendar.getInstance().getTime());

        graph = gexf.getGraph()
                .setDefaultEdgeType(EdgeType.DIRECTED)
                .setIDType(IDType.STRING)
                .setMode(Mode.DYNAMIC)
                .setTimeType(TimeFormat.DOUBLE);

        nodeAttributes = NodeAttributes.builder()
                .withTopics()
                .withHopCount()
                .withDuplicateCount()
                .build();

        graph.getAttributeLists().add(nodeAttributes.getAttributes());

        disseminationEdges = HashMultimap.create();

        File[] dirContents = getDir(directory, protocolName);
        double reportingIntervalCount = 0;
        Report overlay = null;
        for (File file : dirContents) {
            overlay = importJsonReport(file);
            PubMessage msg = overlay.getPublication(messageId);
            if (msg == null) {
                continue;
            }
            addNodes(reportingIntervalCount, overlay, msg.getTopicId());
            removeDeadNodes(reportingIntervalCount, overlay);
            addDisseminationEdges(reportingIntervalCount, overlay, messageId);

            reportingIntervalCount++;
        }

        // Add remaining dissemination edges if the number of animation steps
        // is longer than the actual number of reporting intervals
        double animationStep = reportingIntervalCount;
        for (; animationStep <= animationLength; animationStep++) {
            createDisseminationEdges(animationStep, overlay);
        }

        for (Edge edge : aliveEdges.values()) {
            edge.setEndValue(animationStep);
        }
        for (Node node : aliveNodes.values()) {
            node.setEndValue(animationStep);
        }
        String filePath = createFilePath(protocolName, messageId);
        exportToGexfFile(filePath, gexf);
    }

    private void clearCaches() {
        aliveNodes.clear();
        aliveEdges.clear();
        deadNodes.clear();
        deadEdges.clear();
        nodeSpells.clear();
        edgeSpells.clear();
    }

    private void addDisseminationEdges(double reportingIntervalCount, Report overlay, String messageId) {
        checkNotNull(overlay, "No overlay found");

        for (EdgeData edgeData : overlay.getEdges().values()) {
            Map<String, PubMessage> publications = edgeData.getPublicationsMessages();

            if (publications == null || publications.isEmpty()) {
                continue;
            }

            PubMessage pubMessage = edgeData.getPublicationsMessages().get(messageId);
            if (pubMessage != null) {
                double animationStep = pubMessage.getHopCount() + reportingIntervalCount;

                if (animationStep > animationLength) {
                    animationLength = animationStep;
                }
                disseminationEdges.put(animationStep, edgeData);
//                Log.debug(LogCategory.GEXF, "Adding dissemination edge: " + edgeData + " at step " + animationStep);
            }
        }
        createDisseminationEdges(reportingIntervalCount, overlay);
    }

    private void createDisseminationEdges(double animationStep, Report overlay) {
        checkNotNull(overlay);

        for (EdgeData edgeData : disseminationEdges.get(animationStep)) {

            if (!aliveEdges.containsKey(edgeData.getId())) {

                Node sourceNode = aliveNodes.get(edgeData.getSourceId());
                Node targetNode = aliveNodes.get(edgeData.getTargetId());

                if (sourceNode == null) {
                    sourceNode = graph.createNode(edgeData.getSourceId())
                            .setLabel(createNodeLabel(overlay, Long.parseLong(edgeData.getSourceId())));
                    aliveNodes.put(sourceNode.getId(), sourceNode);
                }

                if (targetNode == null) {
                    targetNode = graph.createNode(edgeData.getTargetId())
                            .setLabel(createNodeLabel(overlay, Long.parseLong(edgeData.getTargetId())));
                    aliveNodes.put(targetNode.getId(), targetNode);
                }

                sourceNode.setEndValue(animationStep);
                targetNode.setEndValue(animationStep);

                Edge edge = sourceNode.connectTo(edgeData.getId(), targetNode)
                        .setLabel(edgeData.getId())
                        .setEdgeType(EdgeType.DIRECTED)
                        .setWeight(1f);

                edge.setStartValue(animationStep);

                aliveEdges.put(edgeData.getId(), edge);
            }
        }
    }

    /**
     * Creates a gexf structural overlay based on data collected from reporter.
     *
     * @param directory    directory of reports
     * @param protocolName name of protocol described by reports
     */
    public void createStructuralOverlay(String directory, String protocolName) {
        Log.info(LogCategory.GEXF, "Creating reports graph");

        clearCaches();

        gexf = new GexfImpl();
        gexf.getMetadata()
                .setCreator("VizPub")
                .setDescription("Structural Overlay")
                .setLastModified(Calendar.getInstance().getTime());

        graph = gexf.getGraph()
                .setDefaultEdgeType(EdgeType.DIRECTED)
                .setIDType(IDType.STRING)
                .setMode(Mode.DYNAMIC)
                .setTimeType(TimeFormat.DOUBLE);

        nodeAttributes = NodeAttributes.builder()
                .withTopics()
                .withSubscriptionSize()
                .withGossipsSent()
                .withGossipsReceived()
                .withKbSent()
                .withKbReceived()
//                .withPublicationsSent()
//                .withPublicationsReceived()
//                .withHitRatio()
//                .withPathLength()
                .withDuplicateCount()
                .build();

        /**
         * Do not add publication to edges as it results in gigantic gexf files sizes
         */
        edgeAttributes = EdgeAttributes.builder()
//                .withTopics()
//                .withPublications()
                .build();

        graph.getAttributeLists().add(nodeAttributes.getAttributes());
        graph.getAttributeLists().add(edgeAttributes.getAttributes());

        File[] dirContents = getDir(directory, protocolName);

        double reportingIntervalCount = 0;
        Report overlay = null;
        for (File file : dirContents) {

            overlay = importJsonReport(file);

            addNodes(reportingIntervalCount, overlay, null);
            removeDeadNodes(reportingIntervalCount, overlay);
            addEdges(reportingIntervalCount);
            removeDeadEdges(reportingIntervalCount);

            reportingIntervalCount++;
        }

        String filePath = createFilePath(protocolName, null);
        exportToGexfFile(filePath, gexf);

        try {
            FileUtils.forceDelete(new File(CollectorWorker.REPORT_PROCESSED));
            FileUtils.forceDelete(new File(CollectorWorker.REPORT_UNPROCESSED));
        } catch (IOException e) {
            e.printStackTrace();
        }
//        calculateAverages(reportingIntervalCount, filePath, overlay.getTopics());
    }


    private File[] getDir(String directory, String protocolName) {
        File dir = new File(directory + protocolName);
        checkNotNull(dir, "Could not find directory " + directory);
        Log.debug(LogCategory.GEXF, "dir: " + dir);

        File[] dirContents = dir.listFiles();

        checkNotNull(dirContents, "Could not find contents of directory " + dir);
        Arrays.sort(dirContents);
        return dirContents;
    }

    private void removeDeadEdges(double reportingIntervalCount) {
        List<String> killEdges = new ArrayList<>();
        for (String edgeId : aliveEdges.keySet()) {

            // this edge is dead
            if (!edges.containsKey(edgeId)) {
                Edge deadEdge = aliveEdges.get(edgeId);
                Spell deadSpell = edgeSpells.get(edgeId);
                deadSpell.setEndValue(reportingIntervalCount);
                deadEdge.getSpells().add(deadSpell);
                deadEdges.put(edgeId, deadEdge);

                // cannot remove edge directly since we are iterating through the collection
                killEdges.add(edgeId);

            }
        }

        // remove the edges safely when done iterating through it
        for (String edgeId : killEdges) {
            aliveEdges.remove(edgeId);
        }
        killEdges.clear();
        edges.clear();
    }

    private void removeDeadNodes(double reportingIntervalCount, Report overlay) {
        List<String> killNodes = new ArrayList<>();
        for (String nodeId : aliveNodes.keySet()) {
            NodeData nodeData = overlay.getNode(nodeId);

            // this node is dead
            if (nodeData == null) {
                Node deadNode = aliveNodes.get(nodeId);
                Spell deadSpell = nodeSpells.get(nodeId);
                deadSpell.setEndValue(reportingIntervalCount);
                deadNode.getSpells().add(deadSpell);
                deadNodes.put(nodeId, deadNode);

                // cannot remove node directly since we are iterating through the collection
                killNodes.add(nodeId);
            }
        }
        for (String nodeId : killNodes) {
            aliveNodes.remove(nodeId);
        }
        killNodes.clear();
    }


    private void addEdges(double reportingIntervalCount) {

        boolean newEdge;
        for (EdgeData edgeData : edges.values()) {

            Node sourceNode = aliveNodes.get(edgeData.getSourceId());
            Node targetNode = aliveNodes.get(edgeData.getTargetId());

            checkNotNull(sourceNode, "Could not find source node in gexf graph");
            checkNotNull(targetNode, "Could not find target node in gexf graph");

            Spell edgeSpell;
            Edge edge;

            // this is an old edge which has returned
            if (deadEdges.containsKey(edgeData.getId())) {
                newEdge = false;
                edge = deadEdges.get(edgeData.getId());
                edgeSpell = new SpellImpl();
                edgeSpell.setStartValue(reportingIntervalCount);
                edge.getSpells().add(edgeSpell);

                deadEdges.remove(edgeData.getId());
                aliveEdges.put(edgeData.getId(), edge);
                edgeSpells.put(edgeData.getId(), edgeSpell);
            }
            // this is an edge which existed previous reporting interval
            else if (aliveEdges.containsKey(edgeData.getId())) {
                newEdge = false;
                edge = aliveEdges.get(edgeData.getId());
                edgeSpell = edgeSpells.get(edgeData.getId());

                // new interval: current interval - next interval
                edgeSpell.setEndValue(reportingIntervalCount + 1);
            }
            // this is a new edge
            else {
                newEdge = true;
                edge = sourceNode.connectTo(edgeData.getId(), targetNode)
                        .setLabel(edgeData.getId())
                        .setEdgeType(EdgeType.DIRECTED)
                        .setWeight(1);

                edgeSpell = new SpellImpl()
                        .setStartValue(reportingIntervalCount)
                        .setEndIntervalType(IntervalType.OPEN);

                edge.getSpells().add(edgeSpell);
                edgeSpells.put(edgeData.getId(), edgeSpell);
                aliveEdges.put(edgeData.getId(), edge);
            }

            checkNotNull(edge, "Could not find edge when adding attributes");

            // only apply topic attribute if the edge is brand new
            if (newEdge) {
                edgeAttributes.update(edge
                        , edgeData
                        , reportingIntervalCount
                        , reportingIntervalCount + 1);
            }
        }
    }

    private void addNodes(double reportingIntervalCount, Report overlay, String topic) {

        double hitRatio = 0.0;
        if (nodeAttributes.includesHitRatio()) {
            hitRatio = GraphAttributes.getHitRatio(overlay);
            Log.debug(LogCategory.COLLECTOR, "Hit-ratio at interval " + reportingIntervalCount + ": " + hitRatio);
        }

        int maxPathLength = 0;
        if (nodeAttributes.includesPathLenghts()) {
            maxPathLength = GraphAttributes.getMaxPathLength(overlay);
            Log.debug(LogCategory.COLLECTOR, "Max path length at interval " + reportingIntervalCount + ": " + maxPathLength);
        }

//        Log.debug(LogCategory.COLLECTOR, "Overlay size : " + overlay.getNodes().size());

        for (NodeData nodeData : overlay.getNodes().values()) {

            // TODO: ugly solution, should refactor
            if (topic != null && !nodeData.subscribesTo(topic)) {
                continue;
            }

            if (topic == null) {

                // derive edges from node neighbors
                for (String neighborId : nodeData.getNeighbors()) {

                    // the neighbor node for edge is down due to churn
                    if (overlay.getNode(neighborId) == null) {
                        continue;
                    }

                    String edgeId = nodeData.getId() + "->" + neighborId;

                    Set<String> topicsForEdge = new HashSet<>();
                    for (String topicId : nodeData.getTopics()) {
                        NodeData neighbor = overlay.getNode(neighborId);
                        if (neighbor.subscribesTo(topicId)) {
                            topicsForEdge.add(topicId);
                        }
                    }

                    EdgeData edgeData = EdgeData.newBuilder()
                            .withId(edgeId)
                            .withSourceId(nodeData.getId())
                            .withTargetId(neighborId)
                            .withTopics(topicsForEdge)
                            .build();

                    edges.put(edgeId, edgeData);
                }
            }

            Spell nodeSpell;
            Node node;

            // this is an old node which has returned
            if (deadNodes.containsKey(nodeData.getId())) {
                node = deadNodes.get(nodeData.getId());
                nodeSpell = new SpellImpl();
                nodeSpell.setStartValue(reportingIntervalCount);
                node.getSpells().add(nodeSpell);

                deadNodes.remove(nodeData.getId());
                aliveNodes.put(nodeData.getId(), node);
                nodeSpells.put(nodeData.getId(), nodeSpell);
            }
            // this is a node which existed previous reporting interval
            else if (aliveNodes.containsKey(nodeData.getId())) {
                node = aliveNodes.get(nodeData.getId());
                nodeSpell = nodeSpells.get(nodeData.getId());

                // new interval: current interval - next interval
                nodeSpell.setEndValue(reportingIntervalCount + 1);
            }
            // this is a new node
            else {
                String nodeLabel = createNodeLabel(overlay, Long.parseLong(nodeData.getId()));

                node = graph.createNode(nodeData.getId())
                        .setLabel(nodeLabel);

                nodeSpell = new SpellImpl()
                        .setStartValue(reportingIntervalCount)
                        .setEndIntervalType(IntervalType.OPEN);

                node.getSpells().add(nodeSpell);
                nodeSpells.put(nodeData.getId(), nodeSpell);
                aliveNodes.put(nodeData.getId(), node);

            }

            checkNotNull(node, "Could not find node when adding node attributes");

            nodeAttributes.update(node
                    , nodeData
                    , reportingIntervalCount
                    , reportingIntervalCount + 1);

            // FIXME: this is a hideous way of determining its a structural overlay being created, and should be refactored
            if (topic == null) {
                nodeAttributes.updateHitRatio(node
                        , hitRatio
                        , reportingIntervalCount
                        , reportingIntervalCount + 1);

                nodeAttributes.updatePathLength(node
                        , maxPathLength
                        , reportingIntervalCount
                        , reportingIntervalCount + 1);
            }
        }
    }

    private String createNodeLabel(Report overlay, Long nodeId) {
        int numberOfDigits = Integer.toString(overlay.getNodes().size()).length();
        String numberFormat = "%0" + numberOfDigits + "d";
        return String.format(numberFormat, nodeId);
    }

    private String createFilePath(String protocolName, String msgId) {
        String timeStamp = DateTime.now().toString("yyyy-MM-dd_HH:mm:ss");
        String[] split = protocolName.split("\\.");
        if (msgId == null) {
            msgId = "";
        }
        String hostName = "";
        try {
            hostName = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        new File("reports/" + hostName + "/").mkdir();
        return "reports/" + hostName + "/" + timeStamp + "_" + split[split.length - 1] + "_" + msgId + ".gexf";
    }

    private void calculateAverages(double reportingIntervalCount, String filePath, Set<String> topics) {
        new GephiProcessing(filePath, reportingIntervalCount)
                .saveNetworkSize()
//                .printNetworkSize()
//                .averageControlMsgs()
//                .averageKb()
//                .saveControlMessages()
//                .printControlMessages()
//                .calculateUndirectedDegree()
//                .saveUndirectedDegree()
//                .calculateOutDegree()
//                .saveOutDegree()
//                .calculateInDegree()
//                .saveInDegree()
//                .calculateOutDegree()
//                .printDegrees()
//                .saveOutDegree()
                .calculateDynamicCentralities()
//                .printDynamicCentralities()
                .saveCentralities()
                .calculateClusteringCoefficient()
//                .printClusteringCoefficient()
                .saveClusteringCoefficient()
//                .averageTopicDiameter(topics)
//                .saveTopicDiameter()
//                .printTopicDiameter()
                .save();
    }

    private Report importJsonReport(File file) {
        Report overlay = null;
        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            Reader reader = new InputStreamReader(fileInputStream);
            Gson gson = new GsonBuilder().registerTypeAdapter(Multimap.class, new MultimapDeserializer()).create();
            overlay = gson.fromJson(reader, Report.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        checkNotNull(overlay, "Could not read reports from json");
        return overlay;
    }

    private void exportToGexfFile(String filePath, it.uniroma1.dis.wsngroup.gexf4j.core.Gexf gexf) {
        StaxGraphWriter graphWriter = new StaxGraphWriter();
        File f = new File(filePath);
        Writer out;
        try {
            out = new FileWriter(f, false);
            graphWriter.writeToStream(gexf, out, "UTF-8");
            Log.info(LogCategory.GEXF, "Writing GEXF file to " + f.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

package no.uio.ifi.vizpub.collector.gexf;

import com.esotericsoftware.minlog.Log;
import com.google.common.base.Stopwatch;
import no.uio.ifi.vizpub.utils.LogCategory;
import org.apache.commons.io.FileUtils;
import org.gephi.data.attributes.api.AttributeController;
import org.gephi.data.attributes.api.AttributeModel;
import org.gephi.data.attributes.api.AttributeType;
import org.gephi.data.attributes.api.Estimator;
import org.gephi.data.attributes.type.DynamicInteger;
import org.gephi.data.attributes.type.Interval;
import org.gephi.dynamic.api.DynamicController;
import org.gephi.dynamic.api.DynamicGraph;
import org.gephi.dynamic.api.DynamicModel;
import org.gephi.filters.api.FilterController;
import org.gephi.filters.api.Query;
import org.gephi.filters.plugin.attribute.AttributeEqualBuilder;
import org.gephi.graph.api.*;
import org.gephi.io.exporter.api.ExportController;
import org.gephi.io.importer.api.Container;
import org.gephi.io.importer.api.EdgeDefault;
import org.gephi.io.importer.api.ImportController;
import org.gephi.io.processor.plugin.DefaultProcessor;
import org.gephi.project.api.ProjectController;
import org.gephi.project.api.Workspace;
import org.gephi.statistics.plugin.ClusteringCoefficient;
import org.gephi.statistics.plugin.Degree;
import org.gephi.statistics.plugin.GraphDistance;
import org.openide.util.Lookup;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Class using the gephi toolkit to calculate normalized structural metrics when building gexf graph.
 *
 * Using the gephi toolkit is more convenient when calculating averages on dynamic attributes. This class
 * is used for this purpose. Not that it is only used to calculate structural properties.
 *
 * @see no.uio.ifi.vizpub.collector.gexf.GexfBuilder
 * @author Nils Peder Korsveien
 */
public class GephiProcessing {
    private final Workspace workspace;
    private final GraphModel graphModel;
    private final String filePath;
    private final double reportingIntervalCount;
    private List<Double> betweennessAverages;
    private List<Double> closenessAverages;
    private List<Double> eccentricityAverages;
    private List<Double> clusteringCoefficientAverages;
    private List<Double> degreeAverages;
    private List<Double> inDegreeAverages;
    private List<Double> outDegreeAverages;
    private List<Double> topicDiameterAverages;

    private List<Double> controlMsgsSentAvg;
    private List<Double> controlMsgsRcvdAvg;
    private List<Double> controlKbSentAvg;
    private List<Double> controlKbRcvdAvg;

    public GephiProcessing(Workspace workspace, GraphModel graphModel, double reportingIntervalCount) {
        betweennessAverages = new ArrayList<>();
        closenessAverages = new ArrayList<>();
        eccentricityAverages = new ArrayList<>();

        clusteringCoefficientAverages = new ArrayList<>();

        topicDiameterAverages = new ArrayList<>();

        degreeAverages = new ArrayList<>();
        inDegreeAverages = new ArrayList<>();
        outDegreeAverages = new ArrayList<>();

        controlMsgsSentAvg = new ArrayList<>();
        controlMsgsRcvdAvg = new ArrayList<>();
        controlKbSentAvg = new ArrayList<>();
        controlKbRcvdAvg = new ArrayList<>();

        this.workspace = workspace;
        this.graphModel = graphModel;
        this.reportingIntervalCount = reportingIntervalCount;
        filePath = null;
    }

    public GephiProcessing(final String filePath, final double reportingIntervalCount) {
        betweennessAverages = new ArrayList<>();
        closenessAverages = new ArrayList<>();
        eccentricityAverages = new ArrayList<>();

        clusteringCoefficientAverages = new ArrayList<>();

        topicDiameterAverages = new ArrayList<>();

        degreeAverages = new ArrayList<>();
        inDegreeAverages = new ArrayList<>();
        outDegreeAverages = new ArrayList<>();

        controlMsgsSentAvg = new ArrayList<>();
        controlMsgsRcvdAvg = new ArrayList<>();
        controlKbSentAvg = new ArrayList<>();
        controlKbRcvdAvg = new ArrayList<>();

        ProjectController pc = Lookup.getDefault().lookup(ProjectController.class);
        pc.newProject();
        workspace = pc.getCurrentWorkspace();

        this.filePath = filePath;
        open();

        graphModel = Lookup.getDefault().lookup(GraphController.class).getModel();
        this.reportingIntervalCount = reportingIntervalCount;
    }

    private void open() {
        ImportController importController = Lookup.getDefault().lookup(ImportController.class);
        Container container;
        try {
            File file = new File(filePath);
            container = importController.importFile(file.getAbsoluteFile());
            container.getLoader().setEdgeDefault(EdgeDefault.DIRECTED);
        } catch (Exception ex) {
            ex.printStackTrace();
            return;
        }
        importController.process(container, new DefaultProcessor(), workspace);
    }

    public void save() {
        ExportController ec = Lookup.getDefault().lookup(ExportController.class);
        try {
            ec.exportFile(new File(filePath));
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Calculates the average number of control messages sent and received.
     *
     * The averages are calculated by dividing the total number of control messages sent/received
     * over the entire execution of the protocol with the number of reporting intervals, i.e. it calculates
     * control messages per reporting interval.
     *
     * @return A pointer to itself for easy client method chaining
     */
    public GephiProcessing averageControlMsgs() {
        AttributeModel am = Lookup.getDefault().lookup(AttributeController.class).getModel();

        am.getNodeTable().addColumn(NodeAttributes.CONTROL_MSGS_SENT_AVG, AttributeType.DOUBLE);
        am.getNodeTable().addColumn(NodeAttributes.CONTROL_MSGS_RECEIVED_AVG, AttributeType.DOUBLE);

        for (double i = 0.0; i < reportingIntervalCount; i++) {
            double high = i + 0.9;
            Integer controlMsgSentCountTotal = 0;
            Integer controlMsgRcvdCountTotal = 0;
            for (Node node : graphModel.getGraph().getNodes()) {

                DynamicInteger gossipsSent = (DynamicInteger) node.getAttributes().getValue(NodeAttributes.CONTROL_MSGS_SENT);
                DynamicInteger gossipsReceived = (DynamicInteger) node.getAttributes().getValue(NodeAttributes.CONTROL_MSGS_RECEIVED);


                if (gossipsSent == null || gossipsReceived == null) {
                    Log.debug(LogCategory.COLLECTOR, "Could not find control msg count attributes");
                    return this;
                }

                Integer controlMsgSentCount =  gossipsSent.getValue(new Interval(i, high));
                Integer controlMsgRcvdCount = gossipsReceived.getValue(new Interval(i, high));

                // null value means the node was down for this interval due to churn
                if (controlMsgRcvdCount != null && controlMsgSentCount != null) {
                    controlMsgSentCountTotal += controlMsgSentCount;
                    controlMsgRcvdCountTotal += controlMsgRcvdCount;
                }

//                double maxInterval = gossipsSent.getHigh();
//                Integer gossipSentCount = gossipsSent.getValue(maxInterval - 1, maxInterval);
//                Integer gossipReceivedCount = gossipsReceived.getValue(maxInterval - 1, maxInterval);

//            Log.debug("node id: " + node.getNodeData().getId());
//            Log.debug("gossipSentCount: " + gossipSentCount);
//            Log.debug("reportingIntervalCount :" + reportingIntervalCount);
//            Log.debug("average: " +  gossipSentCount / reportingIntervalCount);

//                node.getAttributes().setValue(NodeAttributes.CONTROL_MSGS_SENT_AVG, Math.round(avgGossipsSent * 100.0) / 100.0);
//                node.getAttributes().setValue(NodeAttributes.CONTROL_MSGS_RECEIVED_AVG, Math.round(avgGossipsReceieved * 100.0) / 100.0);
            }
            double avgGossipsSent = (double) controlMsgSentCountTotal / graphModel.getGraph().getNodeCount();
            double avgGossipsReceieved = (double) controlMsgRcvdCountTotal / graphModel.getGraph().getNodeCount();

            controlMsgsSentAvg.add(avgGossipsSent);
            controlMsgsRcvdAvg.add(avgGossipsReceieved);
        }
        return this;
    }

    /**
     * Calculates the average number of control messages sent/received measured in kiloBytes
     *
     * The averages are calculated by dividing the total number of kiloBytes of control messages sent/received
     * over the entire execution of the protocol with the number of reporting intervals, i.e. it calculates
     * KB per reporting interval.
     *
     * @return A pointer to itself for easy client method chaining
     */
    public GephiProcessing averageKb() {
//        AttributeModel am = Lookup.getDefault().lookup(AttributeController.class).getModel();

//        am.getNodeTable().addColumn(NodeAttributes.KB_SENT_AVG, AttributeType.DOUBLE);
//        am.getNodeTable().addColumn(NodeAttributes.KB_RECEIVED_AVG, AttributeType.DOUBLE);

        for (double i = 0.0; i < reportingIntervalCount; i++) {
            double high = i + 0.9;
            Integer bitsSentCountTotal = 0;
            Integer bitsReceivedCountTotal = 0;
            for (Node node : graphModel.getGraph().getNodes()) {

                DynamicInteger bitsSent = (DynamicInteger) node.getAttributes().getValue(NodeAttributes.KB_SENT);
                DynamicInteger bitsReceived = (DynamicInteger) node.getAttributes().getValue(NodeAttributes.KB_RECEIVED);

//                double maxInterval = bitsSent.getHigh();
//                Integer bitsSentCount = bitsSent.getValue(maxInterval - 1, maxInterval);
//                Integer bitsReceivedCount = bitsReceived.getValue(maxInterval - 1, maxInterval);

                Integer bitsSentCount = bitsSent.getValue(new Interval(i, high), Estimator.AVERAGE);
                Integer bitsReceivedCount = bitsReceived.getValue(new Interval(i, high), Estimator.AVERAGE);

                if (bitsSentCount != null && bitsReceivedCount != null) {
                    bitsSentCountTotal += bitsSentCount;
                    bitsReceivedCountTotal += bitsReceivedCount;
                }

//            node.getAttributes().setValue(NodeAttributes.KB_SENT_AVG, Math.round(avgBitsSent * 100.0) / 100.0);
//            node.getAttributes().setValue(NodeAttributes.KB_RECEIVED_AVG, Math.round(avgBitsRecv * 100.0) / 100.0);
            }
            double avgBitsSent = (double) bitsSentCountTotal / graphModel.getGraph().getNodeCount();
            double avgBitsRecv = (double) bitsReceivedCountTotal / graphModel.getGraph().getNodeCount();

            controlKbSentAvg.add(avgBitsSent);
            controlKbRcvdAvg.add(avgBitsRecv);
        }
        return this;
    }

    public GephiProcessing calculateDynamicCentralities() {
        Stopwatch stopwatch = Stopwatch.createStarted();
        AttributeModel am = Lookup.getDefault().lookup(AttributeController.class).getModel();
        GraphDistance graphDistance = new GraphDistance();
        DynamicModel dynamicModel = Lookup.getDefault().lookup(DynamicController.class).getModel();
        Log.info(LogCategory.GEXF, "Calculating average centralities...");
        for (double i = 0.0; i < reportingIntervalCount; i++) {
            double high = i + 1;
            DynamicGraph dynamicGraph = dynamicModel.createDynamicGraph(graphModel.getGraph());
            Graph subGraph = dynamicGraph.getSnapshotGraph(i, high);
            graphDistance.execute(graphModel, am);

            double betweennessSum = 0.0;
            double closenessSum = 0.0;
            double eccentricitySum = 0.0;
            for (Node node : subGraph.getNodes()) {
                betweennessSum += (double) node.getAttributes().getValue(GraphDistance.BETWEENNESS);
                closenessSum += (double) node.getAttributes().getValue(GraphDistance.CLOSENESS);
                eccentricitySum += (double) node.getAttributes().getValue(GraphDistance.ECCENTRICITY);
            }
            Double averageBetweenness = betweennessSum / subGraph.getNodeCount();
            Double averageCloseness = closenessSum / subGraph.getNodeCount();
            Double averageEccentricity =  eccentricitySum / subGraph.getNodeCount();

            betweennessAverages.add(averageBetweenness);
            closenessAverages.add(averageCloseness);
            eccentricityAverages.add(averageEccentricity);
        }
        long timeElapsed = stopwatch.elapsed(TimeUnit.SECONDS);
        Log.info(LogCategory.GEXF, "...Finished! (elapsed time: " + timeElapsed + "s");
        return this;
    }

    public GephiProcessing calculateClusteringCoefficient() {
        Stopwatch stopwatch = Stopwatch.createStarted();
        AttributeModel am = Lookup.getDefault().lookup(AttributeController.class).getModel();
        ClusteringCoefficient clusteringCoefficient = new ClusteringCoefficient();
        DynamicModel dynamicModel = Lookup.getDefault().lookup(DynamicController.class).getModel();
        Log.info(LogCategory.GEXF, "Calculating average clustering coefficient...");
        for (double i = 0.0; i < reportingIntervalCount; i++) {
            double high = i + 0.9;
            DynamicGraph dynamicGraph = dynamicModel.createDynamicGraph(graphModel.getGraph());
            Graph subGraph = dynamicGraph.getSnapshotGraph(i, high);
            clusteringCoefficient.execute(graphModel, am);

            double sum = 0.0;
            for (Node node : subGraph.getNodes()) {
                sum += (double) node.getAttributes().getValue(ClusteringCoefficient.CLUSTERING_COEFF);
            }
            Double avg = sum / subGraph.getNodeCount();
            clusteringCoefficientAverages.add(avg);
        }
        long timeElapsed = stopwatch.elapsed(TimeUnit.SECONDS);
        Log.info(LogCategory.GEXF, "...Finished! (elapsed time: " + timeElapsed + "s");
        return this;
    }

    public GephiProcessing calculateUndirectedDegree() {
        Stopwatch stopwatch = Stopwatch.createStarted();
        AttributeModel am = Lookup.getDefault().lookup(AttributeController.class).getModel();
        Degree degree = new Degree();
        DynamicModel dynamicModel = Lookup.getDefault().lookup(DynamicController.class).getModel();
        Log.info(LogCategory.GEXF, "Calculating averages for degree...");
        for (double i = 0.0; i < reportingIntervalCount; i++) {
            double high = i + 0.9;
            DynamicGraph dynamicGraph = dynamicModel.createDynamicGraph(graphModel.getGraph());
            Graph subGraph = dynamicGraph.getSnapshotGraph(i, high);
            degree.execute(graphModel, am);

            int degreeSum = 0;
            for (Node node : subGraph.getNodes()) {
                degreeSum += (int) node.getAttributes().getValue(Degree.DEGREE);
            }
            Double avgDegree = (double) degreeSum / subGraph.getNodeCount();
            degreeAverages.add(avgDegree);
        }
        long timeElapsed = stopwatch.elapsed(TimeUnit.SECONDS);
        Log.info(LogCategory.GEXF, ".. Finised (elapsed time: " + timeElapsed + "s");
        return this;
    }

    public GephiProcessing calculateInDegree() {
        Stopwatch stopwatch = Stopwatch.createStarted();
        AttributeModel am = Lookup.getDefault().lookup(AttributeController.class).getModel();
        Degree degree = new Degree();
        DynamicModel dynamicModel = Lookup.getDefault().lookup(DynamicController.class).getModel();
        Log.info(LogCategory.GEXF, "Calculating averages for In-Degree...");
        for (double i = 0.0; i < reportingIntervalCount; i++) {
            double high = i + 0.9;
            DynamicGraph dynamicGraph = dynamicModel.createDynamicGraph(graphModel.getGraph());
            Graph subGraph = dynamicGraph.getSnapshotGraph(i, high);
            degree.execute(graphModel, am);

            int inDegreeSum = 0;
            for (Node node : subGraph.getNodes()) {
                inDegreeSum += (int) node.getAttributes().getValue(Degree.INDEGREE);
            }
            Double avgInDegree = (double) inDegreeSum / subGraph.getNodeCount();

            inDegreeAverages.add(avgInDegree);
        }
        long timeElapsed = stopwatch.elapsed(TimeUnit.SECONDS);
        Log.info(LogCategory.GEXF, ".. Finised (elapsed time: " + timeElapsed + "s");
        return this;
    }

    public GephiProcessing calculateOutDegree() {
        Stopwatch stopwatch = Stopwatch.createStarted();
        AttributeModel am = Lookup.getDefault().lookup(AttributeController.class).getModel();
        Degree degree = new Degree();
        DynamicModel dynamicModel = Lookup.getDefault().lookup(DynamicController.class).getModel();
        Log.info(LogCategory.GEXF, "Calculating averages for Out-degree...");
        for (double i = 0.0; i < reportingIntervalCount; i++) {
            double high = i + 0.9;
            DynamicGraph dynamicGraph = dynamicModel.createDynamicGraph(graphModel.getGraph());
            Graph subGraph = dynamicGraph.getSnapshotGraph(i, high);
            degree.execute(graphModel, am);

            int outDegreeSum = 0;
            for (Node node : subGraph.getNodes()) {
                outDegreeSum += (int) node.getAttributes().getValue(Degree.OUTDEGREE);
            }
            Double avgOutDegree = (double) outDegreeSum / subGraph.getNodeCount();

            outDegreeAverages.add(avgOutDegree);
        }
        long timeElapsed = stopwatch.elapsed(TimeUnit.SECONDS);
        Log.info(LogCategory.GEXF, ".. Finised (elapsed time: " + timeElapsed + "s");
        return this;
    }

    public GephiProcessing saveCentralities() {
        String hostName =  getHostName();
        saveCsv(closenessAverages, new File("reports/" + hostName + "/closeness.csv"));
        saveCsv(betweennessAverages, new File("reports/" + hostName + "/betweenness.csv"));
        saveCsv(eccentricityAverages, new File("reports/" + hostName + "/eccentricity.csv"));
        return this;
    }

    private String getHostName() {
        String hostName = "";
        try {
            hostName = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        return hostName;
    }

    public GephiProcessing saveTopicDiameter() {
        saveCsv(topicDiameterAverages, new File("reports/" + getHostName() + "/topicDiameter.csv"));
        return this;
    }

    public GephiProcessing saveControlMessages() {
        String hostName = getHostName();
        saveCsv(controlKbRcvdAvg, new File("reports/" + hostName + "/controlKbRcvd.csv"));
        saveCsv(controlKbSentAvg, new File("reports/" + hostName + "/controlKbSent.csv"));
        saveCsv(controlMsgsRcvdAvg, new File("reports/" + hostName + "/controlMessagesRcvd.csv"));
        saveCsv(controlMsgsSentAvg, new File("reports/" + hostName + "/controlMessagesSent.csv"));
        return this;
    }

    public GephiProcessing saveUndirectedDegree() {
        saveCsv(degreeAverages, new File("reports/" + getHostName() + "/Degree.csv"));
        return this;
    }

    public GephiProcessing saveOutDegree() {
        saveCsv(outDegreeAverages, new File("reports/" + getHostName() + "/outDegree.csv"));
        return this;
    }

    public GephiProcessing saveInDegree() {
        saveCsv(inDegreeAverages, new File("reports/" +  getHostName() + "/inDegree.csv"));
        return this;
    }

    public GephiProcessing saveClusteringCoefficient() {
        saveCsv(clusteringCoefficientAverages, new File("reports/" + getHostName() + "/clusteringCoefficient.csv"));
        return this;
    }

    public GephiProcessing saveNetworkSize() {
        Stopwatch stopwatch = Stopwatch.createStarted();
        DynamicModel dynamicModel = Lookup.getDefault().lookup(DynamicController.class).getModel();
        List<Double> networkSize = new ArrayList<>();
        Log.info(LogCategory.GEXF, "Saving network size...");
        for (double i = 0.0; i < reportingIntervalCount; i++) {
            double high = i + 0.5;
            DynamicGraph dynamicGraph = dynamicModel.createDynamicGraph(graphModel.getGraph());
            Graph subGraph = dynamicGraph.getSnapshotGraph(i, high);
            networkSize.add((double) subGraph.getNodeCount());
        }
        saveCsv(networkSize, new File("reports/" + getHostName() + "/networkSize.csv"));
        long timeElapsed = stopwatch.elapsed(TimeUnit.SECONDS);
        Log.info(LogCategory.GEXF, "...Finished! (elapsed time: " + timeElapsed + "s");
        return this;
    }

    private void saveCsv(List<Double> averages, File csvFile) {
        int i = 0;
        StringBuilder builder = new StringBuilder();
        for (Double average : averages) {
            builder.append(i++ + ";" + average + "\n");
        }
        try {
            FileUtils.writeStringToFile(csvFile, builder.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public GephiProcessing printControlMessages() {
        int i = 0;
        System.out.println("Avg Control messages sent");
        System.out.println("--------------------");
        for (Double average : controlMsgsSentAvg) {
            System.out.println(i++ + ";" + average);
        }
        i = 0;
        System.out.println("Avg Control messages received");
        System.out.println("--------------------");
        for (Double average : controlMsgsRcvdAvg) {
            System.out.println(i++ + ";" + average);
        }
        i = 0;
        System.out.println("Avg Control KBs sent");
        System.out.println("--------------------");
        for (Double average : controlKbSentAvg) {
            System.out.println(i++ + ";" + average);
        }
        i = 0;
        System.out.println("Avg Control KBs received");
        System.out.println("--------------------");
        for (Double average : controlKbRcvdAvg) {
            System.out.println(i++ + ";" + average);
        }
        return this;
    }

    public GephiProcessing printDynamicCentralities() {
        int i = 0;
        System.out.println("Avg Betweenness");
        System.out.println("--------------------");
        for (Double average : betweennessAverages) {
            System.out.println(i++ + ";" + average);
        }
        i = 0;
        System.out.println("Avg Closeness");
        System.out.println("--------------------");
        for (Double average : closenessAverages) {
            System.out.println(i++ + ";" + average);
        }
        i = 0;
        System.out.println("Avg Eccentricity");
        System.out.println("--------------------");
        for (Double average : eccentricityAverages) {
            System.out.println(i++ + ";" + average);
        }
        return this;
    }

    public GephiProcessing printClusteringCoefficient() {
        int i = 0;
        System.out.println("Avg CC");
        System.out.println("--------------------");
        for (Double average : clusteringCoefficientAverages) {
            System.out.println(i++ + ";" + average);
        }
        return this;
    }

    public GephiProcessing printDegrees() {
        int i = 0;
        System.out.println("Avg. degree");
        System.out.println("--------------------");
        for (Double average : degreeAverages) {
            System.out.println(i++ + ";" + average);
        }
        i = 0;
        System.out.println("Avg. in-degree");
        System.out.println("--------------------");
        for (Double average : inDegreeAverages) {
            System.out.println(i++ + ";" + average);
        }
        i = 0;
        System.out.println("Avg. out-degree");
        System.out.println("--------------------");
        for (Double average : outDegreeAverages) {
            System.out.println(i++ + ";" + average);
        }
        return this;
    }

    public GephiProcessing printTopicDiameter() {
        int i = 0;
        System.out.println("Avg. topic diameter");
        System.out.println("--------------------");
        for (Double average : topicDiameterAverages) {
            System.out.println(i++ + ";" + average);
        }
        return this;
    }

    public GephiProcessing printNetworkSize() {
        DynamicModel dynamicModel = Lookup.getDefault().lookup(DynamicController.class).getModel();
        System.out.println("Network size");
        System.out.println("--------------------");
        for (double i = 0.0; i < reportingIntervalCount; i++) {
            double high = i + 0.9;
            DynamicGraph dynamicGraph = dynamicModel.createDynamicGraph(graphModel.getGraph());
            Graph subGraph = dynamicGraph.getSnapshotGraph(i, high);
            System.out.println((int)i++ + ";" + subGraph.getNodeCount());
        }
        return this;
    }

    public GephiProcessing averageTopicDiameter(Set<String> topics) {

        Stopwatch stopwatch = Stopwatch.createStarted();
        final AttributeModel am = Lookup.getDefault().lookup(AttributeController.class).getModel();
        final FilterController filterController = Lookup.getDefault().lookup(FilterController.class);
        final DynamicModel dynamicModel = Lookup.getDefault().lookup(DynamicController.class).getModel();
        DynamicGraph dynamicGraph = dynamicModel.createDynamicGraph(graphModel.getGraph());
        final int topicSize = topics.size();

        for (double i = 0.0; i < reportingIntervalCount; i++) {
            Log.info(LogCategory.GEXF, "Calculating topic diameter for interval " + i + "...");
            double diameter = 0;
            double high = i + 1;
            Graph graph = dynamicGraph.getSnapshotGraph(i, high);

//            Log.debug(LogCategory.GEXF, "Node count: " + graph.getNodeCount());
//            Log.debug(LogCategory.GEXF, "Edge count: " + graph.getEdgeCount());

            for (String topic : topics) {


                AttributeEqualBuilder.EqualStringFilter nodeFilter =
                        new AttributeEqualBuilder
                                .EqualStringFilter(am.getNodeTable().getColumn(NodeAttributes.TOPICS));

                AttributeEqualBuilder.EqualStringFilter edgeFilter =
                        new AttributeEqualBuilder
                                .EqualStringFilter(am.getNodeTable().getColumn(EdgeAttributes.TOPICS));

                final String pattern = ".*" + topic + ".*";

                nodeFilter.init(graph);
                edgeFilter.init(graph);

                nodeFilter.setUseRegex(true);
                edgeFilter.setUseRegex(true);

                nodeFilter.setPattern(pattern);
                edgeFilter.setPattern(pattern);

                nodeFilter.evaluate(graph, graph);
                nodeFilter.finish();

                edgeFilter.evaluate(graph, graph);
                edgeFilter.finish();

                Query nodeQuery = filterController.createQuery(nodeFilter);
                Query edgeQuery = filterController.createQuery(edgeFilter);

                filterController.setSubQuery(nodeQuery, edgeQuery);
                GraphView view = filterController.filter(edgeQuery);
                graphModel.setVisibleView(view);
                HierarchicalGraph subGraph = graphModel.getHierarchicalGraphVisible();

//            Log.debug(LogCategory.GEXF, "Topic graph " + topic + " node count: " + subGraph.getNodeCount());
//            Log.debug(LogCategory.GEXF, "Topic graph " + topic + " edge count: " + subGraph.getEdgeCount());

                GraphDistance graphDistance = new GraphDistance();
                graphDistance.execute(subGraph, am);
                diameter += graphDistance.getDiameter();
            }
            double averageDiameter = diameter / topicSize;
//        averageDiameter = (averageDiameter * 100.0) / 100.0;
            topicDiameterAverages.add(averageDiameter);
        }
        long timeElapsed = stopwatch.elapsed(TimeUnit.SECONDS);
        Log.info(LogCategory.GEXF, "...Finished! (elapsed time: " + timeElapsed + "s");
        return this;
    }

}


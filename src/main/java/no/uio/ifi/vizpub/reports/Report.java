package no.uio.ifi.vizpub.reports;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Report representing the state of the system overlay
 *
 * This class describes the final report which is sent to the collector by the reporter. The reporter
 * will create this report by creating nodes and edges based on the data reported through the
 * reportable interface. Each of these report will describe the local overlay (which migh consist of only one node) at
 * the current reporting interval.
 *
 * This class will then be serialized and sent to the collector component, which will handle offline processing of reported
 * data after the experiment is finished.
 *
 * @see no.uio.ifi.reporter.Reporter
 * @see no.uio.ifi.vizpub.Reportable
 * @see NodeData
 * @see EdgeData
 * @see no.uio.ifi.vizpub.reports.PubMessage
 * @author Nils Peder Korsveien
 */

public final class Report {
    private final int protocolId;
    private final String protocolName;
    private final int intervalCount;
    private final Map<String, NodeData> nodes;
    private final Map<String, EdgeData> edges;
    private final Map <String, PubMessage> publications;

    private Report(Builder builder) {
        protocolId = builder.protocolId;
        protocolName = builder.protocolName;
        intervalCount = builder.intervalCount;
        nodes = builder.nodes;
        edges = builder.edges;
        publications = builder.publications;
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean isEmpty() {
        return nodes == null || nodes.isEmpty();
    }

    /**
     * Necessary for creating a poison pill in the reporter service message queue
     *
     * @see peernet.vizpub.reporter.ReporterService
     */
    public Report() {
        protocolId = -1;
        protocolName = "";
        intervalCount = 0;
        nodes = new HashMap<>();
        edges = new HashMap<>();
        publications = new HashMap();
    }

    public int getProtocolId() {
        return protocolId;
    }

    public String getProtocolName() {
        return protocolName;
    }

    public Map<String, NodeData> getNodes() {
        return nodes;
    }

    public Map<String, EdgeData> getEdges() {
        return edges;
    }

    public Map<String, PubMessage> getPublications() {
        return publications;
    }

    public NodeData getNode(String nodeId) {
        return nodes.get(nodeId);
    }

    public EdgeData getEdge(String edgeId) {
        return edges.get(edgeId);
    }

    public PubMessage getPublication(String pubId) {
        return publications.get(pubId);
    }

    public int getIntervalCount() {
        return intervalCount;
    }

    public Set<String> getTopics() {
        Set<String> topics = new HashSet<>();
        for (NodeData nodeData : nodes.values()) {
            topics.addAll(nodeData.getTopics());
        }
        return topics;
    }

    public static final class Builder {
        private int protocolId;
        private String protocolName;
        private int intervalCount;
        private Map<String, NodeData> nodes;
        private Map<String, EdgeData> edges;
        private Map<String, PubMessage> publications;

        private Builder() {
            nodes = new HashMap<>();
            edges = new HashMap<>();
            publications = new HashMap<>();
        }

        public Builder withProtocolId(int protocolId) {
            this.protocolId = protocolId;
            return this;
        }

        public Builder withProtocolName(String protocolName) {
            this.protocolName = protocolName;
            return this;
        }

        public Builder withIntervalCount(int intervalCount) {
            this.intervalCount = intervalCount;
            return this;
        }

        public Builder withNodes(Map<String, NodeData> nodes) {
            this.nodes = nodes;
            return this;
        }

        public Builder withEdges(Map<String, EdgeData> edges) {
            this.edges = edges;
            return this;
        }

        public Builder withPublications(Map<String, PubMessage> publications) {
            this.publications = publications;
            return this;
        }

        public Builder addNodes(Map<String, NodeData> nodes) {
            this.nodes.putAll(nodes);
            return this;
        }

        public Builder addEdges(Map<String, EdgeData> edges) {
            this.edges.putAll(edges);
            return this;
        }

        public Builder addPublications(Map<String, PubMessage> publications) {
            this.publications.putAll(publications);
            return this;
        }

        public Builder addNode(NodeData node) {
            checkArgument(node.getId() != null, "Node has no id");
            this.nodes.put(node.getId(), node);
            return this;
        }

        public Builder addEdge(EdgeData edge) {
            checkArgument(edge.getId() != null, "Edge has no id");
            this.edges.put(edge.getId(), edge);
            return this;
        }

        public Report build() {
            return new Report(this);
        }
    }
}

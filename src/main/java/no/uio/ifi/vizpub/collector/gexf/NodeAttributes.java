package no.uio.ifi.vizpub.collector.gexf;

import it.uniroma1.dis.wsngroup.gexf4j.core.IntervalType;
import it.uniroma1.dis.wsngroup.gexf4j.core.Mode;
import it.uniroma1.dis.wsngroup.gexf4j.core.Node;
import it.uniroma1.dis.wsngroup.gexf4j.core.data.Attribute;
import it.uniroma1.dis.wsngroup.gexf4j.core.data.AttributeClass;
import it.uniroma1.dis.wsngroup.gexf4j.core.data.AttributeList;
import it.uniroma1.dis.wsngroup.gexf4j.core.data.AttributeType;
import it.uniroma1.dis.wsngroup.gexf4j.core.impl.data.AttributeListImpl;
import no.uio.ifi.vizpub.reports.NodeData;
import no.uio.ifi.vizpub.reports.PubMessage;

import java.util.HashMap;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Class responsible for creating and updating gexf node attributes.
 * <p/>
 * When building a dynamic graph, the reported node data must be translated into dynamic gexf attributes in
 * order to be read by the visualization unit. The collector will iterate
 * through every node from each of the reported overlays. Each reports describes the system state at a specific
 * reporting interval. This class will update the value of the node attribute based on the node data derived from
 * the corresponding report.
 *
 * @author Nils Peder Korsveien
 * @see GexfBuilder
 */
public class NodeAttributes {

    public static final String TOPICS = "Topics";
    public static final String SUBSCRIPTION_SIZE = "Subscription Size";
    public static final String CONTROL_MSGS_SENT = "Gossips Sent";
    public static final String CONTROL_MSGS_RECEIVED = "Gossips Received";
    public static final String KB_SENT = "Kb Sent";
    public static final String KB_RECEIVED = "Kb Received";

    public static final String CONTROL_MSGS_SENT_AVG = "Gossips Sent (Avg.)";
    public static final String CONTROL_MSGS_RECEIVED_AVG = "Gossips Received (Avg.)";
    public static final String KB_SENT_AVG = "Kb Sent (Avg.)";
    public static final String KB_RECEIVED_AVG = "Kb Received (Avg.)";

    public static final String PUBLICATIONS_SENT = "Publications Sent";
    public static final String PUBLICATIONS_RECEIVED = "Publications Received";

    public static final String HOP_COUNT = "Hop Count";

    public static final String HIT_RATIO = "Hit Ratio";
    public static final String PATH_LENGTH = "Path Length";
    public static final String DUPLICATE_MESSAGES = "Duplicate Messages";
    public static final String TOPIC_DIAMETER = "Topic Diameter";

    private final AttributeList attributes;
    private final Attribute topics;
    private final Attribute subscriptionSize;
    private final Attribute gossipsSent;
    private final Attribute gossipsReceived;
    private final Attribute kbSent;
    private final Attribute kbReceived;
    private final Attribute publicationsSent;
    private final Attribute publicationsReceived;
    private final Attribute duplicateCount;

    // global attributes

    private final Attribute hitRatio;
    private final Attribute pathLength;

    /**
     * Key: pubMessage id
     * Value: hop count attribute for id
     */
    private final Map<String, Attribute> hopCount;

    private NodeAttributes(Builder builder) {
        attributes = builder.attributes;
        topics = builder.topics;
        subscriptionSize = builder.subscriptionSize;
        gossipsSent = builder.gossipsSent;
        gossipsReceived = builder.gossipsReceived;
        kbSent = builder.kbSent;
        kbReceived = builder.kbReceived;
        publicationsSent = builder.publicationsSent;
        publicationsReceived = builder.publicationsReceived;
        hopCount = builder.hopCount;
        duplicateCount = builder.duplicateCount;

        hitRatio = builder.hitRatio;

        pathLength = builder.pathLength;

    }

    /**
     * Updates a all node attributes for a given gexf node based on the reported node.
     * <p/>
     * This method will only update attributes if they are included by the client code when building
     * the gexf graph.
     *
     * @param node     the gexf node to update
     * @param nodeData the reported node
     * @param start    start value of the current reporting interval
     * @param end      end value of the current reporting interval
     * @see GexfBuilder
     */
    public void update(Node node, NodeData nodeData, double start, double end) {
        if (topics != null) {
            updateTopics(node, nodeData, start, end);
        }
        if (subscriptionSize != null) {
            updateSubscriptionSize(node, nodeData, start, end);
        }
        if (gossipsReceived != null &&
                gossipsSent != null) {
            updateGossipCount(node, nodeData, start, end);
        }
        if (kbSent != null &&
                kbReceived != null) {
            updateKbCount(node, nodeData, start, end);
        }
        if (publicationsSent != null &&
                publicationsReceived != null) {
            updatePublications(node, nodeData, start, end);
        }
        if (hopCount != null) {
            updateHopCount(node, nodeData, start, end);
        }
        if (duplicateCount != null) {
            updateDuplicateCount(node, nodeData, start, end);
        }
    }

    private void updateDuplicateCount(Node node, NodeData nodeData, double start, double end) {
        checkNotNull(node);
        checkArgument(start < end, "Start value must be lower than end value");

        String duplicateCountValue = Integer.toString(nodeData.getDuplicateCount());

//        Log.debug(LogCategory.GEXF, "Duplicate message count " + duplicateCountValue);

        node.getAttributeValues()
                .createValue(duplicateCount, duplicateCountValue)
                .setStartValue(start)
                .setEndValue(end)
                .setEndIntervalType(IntervalType.OPEN);
    }

    public void updateHitRatio(Node node, double currentHitRatio,  double start, double end) {

        checkNotNull(node);
        checkArgument(start < end, "Start value must be lower than end value");

//        checkNotNull(hitRatio, "This attribute object does not include hit-ratio");
        if (hitRatio == null) {
            return;
        }

        String hitRatioValue = Double.toString(currentHitRatio);
        checkNotNull(hitRatioValue, "Hit ratio has not been calculated");

        node.getAttributeValues()
                .createValue(hitRatio, hitRatioValue)
                .setStartValue(start)
                .setEndValue(end)
                .setEndIntervalType(IntervalType.OPEN);
    }

    public void updatePathLength(Node node, int currentPathLength, double start, double end) {
        checkNotNull(node);
        checkArgument(start < end, "Start value must be lower than end value");

        if (pathLength == null) {
            return;
        }
//        checkNotNull(pathLength, "This attribute object does not include path length");
//

        String pathLengthValue = Integer.toString(currentPathLength);
        checkNotNull(pathLengthValue, "Hit ratio has not been calculated");

        node.getAttributeValues()
                .createValue(pathLength, pathLengthValue)
                .setStartValue(start)
                .setEndValue(end)
                .setEndIntervalType(IntervalType.OPEN);
    }

    /**
     * Updates the hop count for a given gexf node based on the received publication for the reported reports node
     *
     * @param node     the gexf node to update
     * @param nodeData the reported node
     * @param start    start value of the current reporting interval
     * @param end      end value of the current reporting interval
     */
    private void updateHopCount(Node node, NodeData nodeData, double start, double end) {
        checkNotNull(node);
        checkNotNull(nodeData);
        if (nodeData.getPublicationMsgsReceived().isEmpty()) {
            return;
        }
        checkArgument(start < end, "Start value must be lower than end value");

        Map<String, PubMessage> pubMessagesReceived = nodeData.getPublicationMsgsReceived();
        for (PubMessage pubMessage : pubMessagesReceived.values()) {
            Attribute hops = hopCount.get(pubMessage.getMsgId());
            if (hops == null) {
                hops = attributes.createAttribute(AttributeType.INTEGER, HOP_COUNT
                        + "P"
                        + pubMessage.getMsgId()
                        + " T"
                        + pubMessage.getTopicId());

                hopCount.put(pubMessage.getMsgId(), hops);
            }

            node.getAttributeValues()
                    .createValue(hops, Integer.toString(pubMessage.getHopCount()))
                    .setStartValue(start + pubMessage.getHopCount());
        }
    }

    /**
     * Updates the publications sent and received for a given gexf node based on the reported reports node
     *
     * @param node     the gexf node to update
     * @param nodeData the reported node
     * @param start    start value of the current reporting interval
     * @param end      end value of the current reporting interval
     */
    private void updatePublications(Node node, NodeData nodeData, double start, double end) {
        checkNotNull(node);
        checkNotNull(nodeData);

        if (nodeData.getPublicationMsgsReceived().isEmpty() ||
                nodeData.getPublicationMsgsSent().isEmpty()) {
            return;
        }
        checkArgument(start < end, "Start value must be lower than end value");

        node.getAttributeValues()
                .createValue(publicationsSent, nodeData.getPublicationMsgsSent().values().toString())
                .setStartValue(start)
                .setEndValue(end)
                .setEndIntervalType(IntervalType.OPEN);

        node.getAttributeValues()
                .createValue(publicationsReceived, nodeData.getPublicationMsgsReceived().values().toString())
                .setStartValue(start)
                .setEndValue(end)
                .setEndIntervalType(IntervalType.OPEN);
    }

    /**
     * Updates the publications sent and received for a given gexf node based on the reported reports node
     *
     * @param node     the gexf node to update
     * @param nodeData the reported node
     * @param start    start value of the current reporting interval
     * @param end      end value of the current reporting interval
     */
    private void updateKbCount(Node node, NodeData nodeData, double start, double end) {
        checkNotNull(node);
        checkNotNull(nodeData);

        if (nodeData.getBitsSent() < 0 || nodeData.getBitsReceived() < 0) {
            return;
        }
        checkArgument(start < end, "Start value must be lower than end value");

        node.getAttributeValues()
                .createValue(kbSent, Integer.toString(nodeData.getBitsSent() / 1000))
                .setStartValue(start)
                .setEndValue(end)
                .setEndIntervalType(IntervalType.OPEN);

        node.getAttributeValues()
                .createValue(kbReceived, Integer.toString(nodeData.getBitsReceived() / 1000))
                .setStartValue(start)
                .setEndValue(end)
                .setEndIntervalType(IntervalType.OPEN);
    }

    /**
     * Updates the publications sent and received for a given gexf node based on the reported reports node
     *
     * @param node     the gexf node to update
     * @param nodeData the reported node
     * @param start    start value of the current reporting interval
     * @param end      end value of the current reporting interval
     */
    private void updateGossipCount(Node node, NodeData nodeData, double start, double end) {
        checkNotNull(node);
        checkNotNull(nodeData);

        if (nodeData.getControlMsgsSent() < 0 || nodeData.getControlMsgsReceived() < 0) {
            return;
        }
        checkArgument(start < end, "Start value must be lower than end value");

        node.getAttributeValues()
                .createValue(gossipsSent, Integer.toString(nodeData.getControlMsgsSent()))
                .setStartValue(start)
                .setEndValue(end)
                .setEndIntervalType(IntervalType.OPEN);

        node.getAttributeValues()
                .createValue(gossipsReceived, Integer.toString(nodeData.getControlMsgsReceived()))
                .setStartValue(start)
                .setEndValue(end)
                .setEndIntervalType(IntervalType.OPEN);
    }

    /**
     * Updates the subscription size for a given gexf node based on the reported reports node
     *
     * @param node     the gexf node to update
     * @param nodeData the reported node
     * @param start    start value of the current reporting interval
     * @param end      end value of the current reporting interval
     */
    private void updateSubscriptionSize(Node node, NodeData nodeData, double start, double end) {
        checkNotNull(node);
        checkNotNull(nodeData);

        if (nodeData.getSubscriptionSize() < 0) {
            return;
        }
        checkArgument(start < end, "Start value must be lower than end value");

        node.getAttributeValues()
                .createValue(subscriptionSize, Integer.toString(nodeData.getSubscriptionSize()));
    }

    /**
     * Updates the topics subscribed to for a given gexf node based on the reported reports node
     *
     * @param node     the gexf node to update
     * @param nodeData the reported node
     * @param start    start value of the current reporting interval
     * @param end      end value of the current reporting interval
     */
    private void updateTopics(Node node, NodeData nodeData, double start, double end) {
        checkNotNull(node);
        checkNotNull(nodeData);

        if (nodeData.getTopics().isEmpty()) {
            return;
        }
        checkArgument(start < end, "Start value must be lower than end value");

        node.getAttributeValues()
                .createValue(topics, nodeData.getTopics().toString());
    }

    public AttributeList getAttributes() {
        return attributes;
    }

    public Attribute getTopics() {
        return topics;
    }

    public Attribute getSubscriptionSize() {
        return subscriptionSize;
    }

    public Attribute getGossipsSent() {
        return gossipsSent;
    }

    public Attribute getGossipsReceived() {
        return gossipsReceived;
    }

    public Attribute getKbSent() {
        return kbSent;
    }

    public Attribute getKbReceived() {
        return kbReceived;
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean includesPathLenghts() {
        return pathLength != null;
    }

    public boolean includesHitRatio() {
        return hitRatio != null;
    }

    public static final class Builder {
        private AttributeList attributes;
        private Attribute topics;
        private Attribute subscriptionSize;
        private Attribute gossipsSent;
        private Attribute gossipsReceived;
        private Attribute kbSent;
        private Attribute kbReceived;
        public Attribute publicationsSent;
        public Attribute publicationsReceived;
        public Map<String, Attribute> hopCount;
        public Attribute duplicateCount;

        private Attribute hitRatio;
        private Attribute pathLength;

        private Builder() {
            this.attributes = new AttributeListImpl(AttributeClass.NODE);
            attributes.setMode(Mode.DYNAMIC);
        }

        public Builder withTopics() {
            topics = this.attributes.createAttribute(AttributeType.STRING, TOPICS);
            return this;
        }

        public Builder withSubscriptionSize() {
            subscriptionSize = this.attributes.createAttribute(AttributeType.INTEGER, SUBSCRIPTION_SIZE);
            return this;
        }

        public Builder withGossipsSent() {
            gossipsSent = this.attributes.createAttribute(AttributeType.INTEGER, CONTROL_MSGS_SENT);
            return this;
        }

        public Builder withGossipsReceived() {
            gossipsReceived = this.attributes.createAttribute(AttributeType.INTEGER, CONTROL_MSGS_RECEIVED);
            return this;
        }

        public Builder withKbSent() {
            kbSent = this.attributes.createAttribute(AttributeType.INTEGER, KB_SENT);
            return this;
        }

        public Builder withKbReceived() {
            kbReceived = this.attributes.createAttribute(AttributeType.INTEGER, KB_RECEIVED);
            return this;
        }

        public Builder withPublicationsSent() {
            publicationsSent = this.attributes.createAttribute(AttributeType.STRING, PUBLICATIONS_SENT);
            return this;
        }

        public Builder withPublicationsReceived() {
            publicationsReceived = this.attributes.createAttribute(AttributeType.STRING, PUBLICATIONS_RECEIVED);
            return this;
        }

        public Builder withHopCount() {
            hopCount = new HashMap<>();
            return this;
        }

        public Builder withDuplicateCount() {
            this.duplicateCount = attributes.createAttribute(AttributeType.INTEGER, DUPLICATE_MESSAGES);
            return this;
        }

        public Builder withHitRatio() {
            this.hitRatio = attributes.createAttribute(AttributeType.DOUBLE, HIT_RATIO);
            return this;
        }

        public Builder withPathLength() {
            this.pathLength = attributes.createAttribute(AttributeType.INTEGER, PATH_LENGTH);
            return this;
        }

        public NodeAttributes build() {
            return new NodeAttributes(this);
        }
    }
}

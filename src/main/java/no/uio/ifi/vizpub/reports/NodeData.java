package no.uio.ifi.vizpub.reports;

import com.google.common.collect.Multimap;

import java.util.Map;
import java.util.Set;

/**
 * Class representing a node reported through the reporter interface
 *
 * These nodes are created by the reporter based on the data collected through the
 * reportable interface. All edges are then added to a final <class>Report</class> class
 * which represents the entire system overlay at the current reporting interval.
 *
 * @see peernet.vizpub.reporter.Reporter
 * @see peernet.vizpub.Reportable
 * @see Report
 *
 * @author Nils Peder Korsveien
 */
public final class NodeData {
    private final String id;
    private final Set<String> neighbors;
    private final Set<String> topics;
    private final int subscriptionSize;
    private final int controlMsgsSent;
    private final int controlMsgsReceived;
    private final int bitsSent;
    private final int bitsReceived;

    private final Map<String, PubMessage> publicationMsgsSent;
    private final Map<String, PubMessage> publicationMsgsReceived;
    private final int duplicateCount;

    private NodeData(Builder builder) {
        id = builder.id;
        neighbors = builder.neighbors;
        topics = builder.topics;
        controlMsgsSent = builder.controlMsgsSent;
        controlMsgsReceived = builder.controlMsgsReceived;
        bitsSent = builder.bitsSent;
        bitsReceived = builder.bitsReceived;
        subscriptionSize = builder.subscriptionSize;
        publicationMsgsSent = builder.publicationMsgsSent;
        publicationMsgsReceived = builder.publicationMsgsReceived;
        duplicateCount = builder.duplicateCount;
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean subscribesTo(String topic) {
        return topics.contains(topic);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        NodeData that = (NodeData) o;

        if (!id.equals(that.id)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "NodeData{" +
                "id='" + id + '\'' +
                '}';
    }

    public String getId() {
        return id;
    }

    public Set<String> getNeighbors() {
        return neighbors;
    }

    public Set<String> getTopics() {
        return topics;
    }

    public int getControlMsgsReceived() {
        return controlMsgsReceived;
    }

    public int getControlMsgsSent() {
        return controlMsgsSent;
    }

    public int getSubscriptionSize() {
        return subscriptionSize;
    }



    public int getBitsSent() {
        return bitsSent;
    }

    public int getBitsReceived() {
        return bitsReceived;
    }

    public Map<String, PubMessage> getPublicationMsgsSent() {
        return publicationMsgsSent;
    }

    public Map<String, PubMessage> getPublicationMsgsReceived() {
        return publicationMsgsReceived;
    }

    public boolean receivedPublication(String msgId) {

        return publicationMsgsReceived.containsKey(msgId);
    }

    public int getDuplicateCount() {
        return duplicateCount;
    }

    public static final class Builder {
        private String id;
        private Set<String> neighbors;
        private Set<String> topics;
        private Multimap<String, String> topicNeighbors;

        private int bitsSent;
        private int bitsReceived;
        private int controlMsgsSent;
        private int controlMsgsReceived;
        private int subscriptionSize;

        private Map<String, PubMessage> publicationMsgsReceived;
        private Map<String, PubMessage> publicationMsgsSent;
        private boolean isUp;
        private int duplicateCount;

        public Builder withId(String id) {
            this.id = id;
            return this;
        }

        public Builder withNeighbors(Set<String> neighbors) {
            this.neighbors = neighbors;
            return this;
        }

        public Builder withTopics(Set<String> topics) {
            this.topics = topics;
            return this;
        }

        public Builder withControlMsgsSent(int count) {
            this.controlMsgsSent = count;
            return this;
        }

        public Builder withControlMsgsReceived(int count) {
            this.controlMsgsReceived = count;
            return this;
        }

        public Builder withBitsSent(int bitsSent) {
            this.bitsSent = bitsSent;
            return this;
        }

        public Builder withBitsReceived(int bitsReceived) {
            this.bitsReceived = bitsReceived;
            return this;
        }

        public Builder withSubscriptionSize(int subscriptionSize) {
            this.subscriptionSize = subscriptionSize;
            return this;
        }

        public Builder withPublicationMsgsSent(Map<String, PubMessage> messages) {
            this.publicationMsgsSent = messages;
            return this;
        }

        public Builder withPublicationMsgsReceived(Map<String, PubMessage> messages) {
            this.publicationMsgsReceived = messages;
            return this;
        }

        public Builder withIsUp(boolean isUp) {
            this.isUp = isUp;
            return this;
        }

        public NodeData build() {
            return new NodeData(this);
        }

        public Builder withDuplicateMsgs(int duplicateCount) {
            this.duplicateCount = duplicateCount;
            return this;
        }
    }
}

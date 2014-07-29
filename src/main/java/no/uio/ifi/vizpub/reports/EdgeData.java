package no.uio.ifi.vizpub.reports;

import java.util.Map;
import java.util.Set;

/**
 * Class representing an edge reported through the reporter interface
 *
 * These edges are built by the reporter based on the data collected through the
 * reportable interface. All edges are then added to a final <class>Report</class> class
 * which represents the entire system overlay at the current reporting interval.
 *
 * @see no.uio.ifi.vizpub.reporter.Reporter
 * @see no.uio.ifi.vizpub.Reportable
 * @see Report
 *
 * @author Nils Peder Korsveien
 */
public final class EdgeData {
    private final String id;
    private final String sourceId;
    private final String targetId;
    private final Set<String> topics;
    private final int controlMsgCount;

    private final Map<String, PubMessage> publicationsMessages;

    private EdgeData(Builder builder) {
        id = builder.id;
        sourceId = builder.sourceId;
        targetId = builder.targetId;
        topics = builder.topics;
        controlMsgCount = builder.contolMsgCount;
        publicationsMessages = builder.publicationMessages;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        EdgeData that = (EdgeData) o;

        if (!id.equals(that.id)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return sourceId + "->" + targetId;
    }

    public String getId() {
        return id;
    }

    public String getSourceId() {
        return sourceId;
    }

    public String getTargetId() {
        return targetId;
    }

    public Set<String> getTopics() {
        return topics;
    }

    public int getControlMsgCount() {
        return controlMsgCount;
    }

    public Map<String, PubMessage> getPublicationsMessages() {
        return publicationsMessages;
    }

    public static final class Builder {
        private String id;
        private String sourceId;
        private String targetId;
        private Set<String> topics;
        private int contolMsgCount;
        public Map<String, PubMessage> publicationMessages;

        private Builder() {
        }

        public Builder withId(String id) {
            this.id = id;
            return this;
        }

        public Builder withSourceId(String sourceId) {
            this.sourceId = sourceId;
            return this;
        }

        public Builder withTargetId(String targetId) {
            this.targetId = targetId;
            return this;
        }

        public Builder withTopics(Set<String> topics) {
            this.topics = topics;
            return this;
        }

        public Builder withControlMsgCount(int gossipMessageCount) {
            this.contolMsgCount = gossipMessageCount;
            return this;
        }

        public Builder withPublicationMessages(Map<String, PubMessage> publicationMessages) {
            this.publicationMessages = publicationMessages;
            return this;
        }

        public EdgeData build() {
            return new EdgeData(this);
        }
    }
}

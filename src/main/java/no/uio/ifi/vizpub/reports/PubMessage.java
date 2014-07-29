package no.uio.ifi.vizpub.reports;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Class describing the structure of publication messages.
 *
 * The member fields and methods of this class is necessary in order to derived properties an metrics
 * in the Collector. More specifically, it is used to create dissemination overlay which can be opened in
 * the visualization unit, where disseminations can be replayed step-by-step and metrics such as hit ratio
 * and hop count can be derived.
 *
 * @author Nils Peder Korsveien
 */
public class PubMessage implements Serializable {

    static final long serialVersionUID = 1664433552139953717L;

    private final String msgId;
    private final String topicId;
    private final String originalSenderId;
    private final String sourceId;
    private final List<String> destinationIds;
    private final int hopCount;

    protected PubMessage(Builder builder) {
        checkNotNull(builder.msgId, "Message id is required");

        msgId = builder.msgId;
        topicId = builder.topicId;
        sourceId = builder.sourceId;
        originalSenderId = builder.originalSenderId;
        destinationIds = builder.destinationIds;
        hopCount = builder.hopCount;
    }

    private PubMessage(String msgId, String topicId, String originalSenderId, String sourceId, List<String> destinationIds, int hopCount) {
        this.msgId = msgId;
        this.topicId = topicId;
        this.originalSenderId = originalSenderId;
        this.sourceId = sourceId;
        this.destinationIds = destinationIds;
        this.hopCount = hopCount;
    }

    public static PubMessage forwardingCopy(PubMessage msg, List<String> forwardingDestinations) {
        return new PubMessage(msg.getMsgId()
                , msg.getTopicId()
                , msg.getOriginalSenderId()
                , msg.getSourceId()
                , forwardingDestinations
                , msg.getHopCount() + 1);
    }

    public static PubMessage incrementAndCopy(PubMessage msg) {
        return new PubMessage(msg.getMsgId()
                , msg.getTopicId()
                , msg.getOriginalSenderId()
                , msg.getSourceId()
                , msg.getDestinationIds()
                , msg.getHopCount() + 1);
    }

    public static PubMessage decrementAndCopy(PubMessage msg) {
        return new PubMessage(msg.getMsgId()
                , msg.getTopicId()
                , msg.getOriginalSenderId()
                , msg.getSourceId()
                , msg.getDestinationIds()
                , msg.getHopCount() - 1);
    }

    @Override
    public String toString() {
        return msgId;
    }

    @Override
    public boolean equals(Object o) {
        PubMessage that = (PubMessage) o;
        return msgId.equals(that.msgId);

    }

    @Override
    public int hashCode() {
        return msgId.hashCode();
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getMsgId() {
        return msgId;
    }

    public String getTopicId() {
        return topicId;
    }

    public String getSourceId() {
        return sourceId;
    }

    public String getOriginalSenderId() {
        return originalSenderId;
    }

    public List<String> getDestinationIds() {
        return destinationIds;
    }

    public int getHopCount() {
        return hopCount;
    }



    public static final class Builder {
        private String msgId;
        private String topicId;
        private String originalSenderId;
        private String sourceId;
        private List<String> destinationIds;
        public int hopCount;

        private Builder() {
            destinationIds = new ArrayList<>();
        }

        public Builder withMsgId(String msgId) {
            this.msgId = msgId;
            return this;
        }

        public Builder withTopicId(String topicId) {
            this.topicId = topicId;
            return this;
        }

        public Builder withOriginalSenderId(String originalSenderId) {
            this.originalSenderId = originalSenderId;
            return this;
        }

        public Builder withSourceId(String sourceId) {
            this.sourceId = sourceId;
            return this;
        }

        public Builder withDestinationIds(List<String> destinationIds) {
            this.destinationIds = destinationIds;
            return this;
        }

        public Builder addDestinationId(String destinationId) {
            this.destinationIds.add(destinationId);
            return this;
        }

        public Builder withHopCount(int hopCount) {
            this.hopCount = hopCount;
            return this;
        }


        public PubMessage build() {
            return new PubMessage(this);
        }
    }
}

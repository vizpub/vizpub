package no.uio.ifi.vizpub.collector.gexf;

import it.uniroma1.dis.wsngroup.gexf4j.core.Edge;
import it.uniroma1.dis.wsngroup.gexf4j.core.IntervalType;
import it.uniroma1.dis.wsngroup.gexf4j.core.Mode;
import it.uniroma1.dis.wsngroup.gexf4j.core.data.Attribute;
import it.uniroma1.dis.wsngroup.gexf4j.core.data.AttributeClass;
import it.uniroma1.dis.wsngroup.gexf4j.core.data.AttributeList;
import it.uniroma1.dis.wsngroup.gexf4j.core.data.AttributeType;
import it.uniroma1.dis.wsngroup.gexf4j.core.impl.data.AttributeListImpl;
import no.uio.ifi.vizpub.reports.EdgeData;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Class responsible for creating and updating gexf edge attributes.
 * <p/>
 * When building a dynamic graph, the reported edge data must be translated into dynamic gexf attributes in
 * order to be read by the visualization unit. The collector will iterate
 * through every edge from each of the reported overlays. Each reports describes the system state at a specific
 * reporting interval. This class will update the value of the edge attribute based on the data derived from
 * the corresponding report.
 *
 * @author Nils Peder Korsveien
 * @see GexfBuilder
 */
public class EdgeAttributes {
    private final AttributeList attributes;
    private final Attribute topics;
    private final Attribute controlMsgCount;
    private final Attribute publications;

    public static final String TOPICS = "Topics";

    private EdgeAttributes(Builder builder) {
        attributes = builder.attributes;
        topics = builder.topics;
        controlMsgCount = builder.gossipCount;
        publications = builder.publications;
    }

    /**
     * Updates a all edge attributes for a given gexf edge based on the reported edge.
     * <p/>
     * This method will only update attributes if they are included by the client code when building
     * the gexf graph.
     *
     * @see GexfBuilder
     * @param edge      the gexf edge to update
     * @param edgeData the reported edge
     * @param start     start value of the current reporting interval
     * @param end       end value of the current reporting interval
     */
    public void update(Edge edge, EdgeData edgeData, double start, double end) {
        if (topics != null) {
            updateTopics(edge, edgeData, start, end);
        }
        if (controlMsgCount != null) {
            updateControlMsgCount(edge, edgeData, start, end);
        }
        if (publications != null) {
            updatePublications(edge, edgeData, start, end);
        }
    }

    /**
     * Updates the publication attribute for a given gexf edge based on the reported edge.
     *
     * The publications attribute for a edge describes which publication messages passed through
     * this edge.
     *
     * @see GexfBuilder
     * @param edge      the gexf edge to update
     * @param edgeData the reported edge
     * @param start     start value of the current reporting interval
     * @param end       end value of the current reporting interval
     */
    private void updatePublications(Edge edge, EdgeData edgeData, double start, double end) {
        checkNotNull(edge);
        checkNotNull(edgeData);
        checkArgument(start < end, "Start value must be lower than end value");

        edge.getAttributeValues()
                .createValue(publications, edgeData.getPublicationsMessages().keySet().toString())
                .setStartValue(start)
                .setEndValue(end)
                .setEndIntervalType(IntervalType.OPEN);
    }

    /**
     * Updates the control message count attribute for a given gexf edge based on the reported edge.
     *
     * The control message count is the number of control messages (such as gossips) which passed through this
     * edge.
     *
     * @see GexfBuilder
     * @param edge      the gexf edge to update
     * @param edgeData the reported edge
     * @param start     start value of the current reporting interval
     * @param end       end value of the current reporting interval
     */
    private void updateControlMsgCount(Edge edge, EdgeData edgeData, double start, double end) {
        checkNotNull(edge);
        checkNotNull(edgeData);

        if (edgeData.getControlMsgCount() < 0) {
            return;
        }
        checkArgument(start < end, "Start value must be lower than end value");

        edge.getAttributeValues()
                .createValue(controlMsgCount, Integer.toString(edgeData.getControlMsgCount()))
                .setStartValue(start)
                .setEndValue(end)
                .setEndIntervalType(IntervalType.OPEN);
    }

    /**
     * Updates the topics attribute for a given gexf edge based on the reported edge.
     *
     * The topics attribute describe which topic the edge is a part of. This is necessary
     * in order to filter the graph into a topic overlay in the visualization unit.
     *
     * @see GexfBuilder
     * @param edge      the gexf edge to update
     * @param edgeData the reported edge
     * @param start     start value of the current reporting interval
     * @param end       end value of the current reporting interval
     */
    private void updateTopics(Edge edge, EdgeData edgeData, double start, double end) {
        checkNotNull(edge);
        checkNotNull(edgeData);

        if (edgeData.getTopics().isEmpty()) {
            return;
        }
        checkArgument(start < end, "Start value must be lower than end value");


        edge.getAttributeValues()
                .createValue(topics, edgeData.getTopics().toString());
    }

    public AttributeList getAttributes() {
        return attributes;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private AttributeList attributes;
        private Attribute topics;
        private Attribute gossipCount;
        private Attribute publications;

        private Builder() {
            attributes = new AttributeListImpl(AttributeClass.EDGE);
            attributes.setMode(Mode.DYNAMIC);
        }

        public Builder withTopics() {
            topics = this.attributes.createAttribute(AttributeType.STRING, TOPICS);
            return this;
        }

        public Builder withGossipCount() {
            gossipCount = this.attributes.createAttribute(AttributeType.INTEGER, "Gossip Count");
            return this;
        }

        public Builder withPublications() {
            publications = this.attributes.createAttribute(AttributeType.STRING, "Publications");
            return this;
        }

        public EdgeAttributes build() {
            return new EdgeAttributes(this);
        }
    }
}

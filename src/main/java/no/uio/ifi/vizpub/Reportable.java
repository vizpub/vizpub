package no.uio.ifi.vizpub;

import com.google.common.collect.Multimap;
import no.uio.ifi.vizpub.reports.PubMessage;

import java.util.Map;
import java.util.Set;


/**
 * Interface for reporting protocol reports state to the vizpub plugin.
 * <p/>
 * This interface should be implemented by all protocols that wants to
 * be compatible with the vizpub plugin. These methods are invoked by the
 * plugin in order to collect data such as
 * neighbors from <Code>Protocol</Code> instances, in order to represent
 * the protocol reports state. More specifically, it is invoked by the
 * <code>Reporter</code> control at a time interval which must be specified
 * in the config file. The Reporter will iterate through all nodes in the <code>Network</code>
 * and execute these API calls on the protocol specified in the <code>protocol</code> parameter
 * which must be defined the config file.
 * <p/>
 * Example config:
 * <p/>
 * <pre>
 *     {@code
 *     include.control Reporter
 *
 *     control.reporter peernet.vizpub.Reporter
 *     {
 *          protocol    cyclon
 *          STEP        1000
 *     }
 *     }
 * </pre>
 * <p/>
 * This example config will execute the reporter on the cyclon protocol at 1000ms intervals (The cyclon protocol must be
 * defined somewhere else in the config). This means the
 * <code>Protocol</code> class executing the cyclon algorithm needs to implement this interface in order to work with the
 * Gephi plugin.
 *
 * @author Nils Peder Korsveien
 */
public interface Reportable {

    /**
     * Reports the unique id of this node.
     *
     * @return id of node
     */
    public String reportId();

    /**
     * Report the unique ids of the neighbors of this node.
     *
     * @return ids of neighbors
     */
    public Set<String> reportNeighborIds();

    /**
     * Report the topics neighbors of this node where key = topic and value = neighborId
     *
     * @return topic neighbor of this node
     */
    public Set<String> reportTopicNeighborIds(int topic);

    /**
     * Report the topics subscribed to by this node.
     *
     * @return topics subscribed to by this node
     */
    public Set<String> reportTopics();

    /**
     * Report the total number of gossip messages sent by this node.
     *
     * @return number of gossip messages sent
     */
    public int reportControlMsgsReceived();

    /**
     * Report the total number of gossip messages received on this node.
     *
     * @return number of gossip messages received
     */
    public int reportControlMsgsSent();

    /**
     * Report the amount of control messages sent in number of bytes
     *
     * @return the total number of bytes sent up to this reporting interval
     */
    public int reportControlBytesSent();

    /**
     * Report the amount of control messages received in number of bytes
     *
     * @return the total number of bytes received up to this reporting interval
     */
    public int reportControlBytesReceived();

    /**
     * Report the subscription size of this node.
     *
     * @return The subscription size of this node
     */
    public int reportSubscriptionSize();

    /**
     * Report the number of gossiping messages which has traveled through an edge.
     *
     * @return map of edgeIds and their control message count
     */
    public Map<String, Integer> reportControlMsgsEdge();

    /**
     * Report publications sent by this node since previous reporting interval
     *
     * @return publications sent since previous reporting interval
     */
    public Map<String, PubMessage> reportPubMsgsSent();

    /**
     * Report publications received by this node since previous reporting interval
     *
     * @return publications received since previous reporting interval
     */
    public Map<String, PubMessage> reportPubMsgsReceived();

    /**
     * Report number of duplicate publication messages received
     *
     * @return number of duplicates
     */
    public int reportDuplicatePubMsgs();

    // ----------------------------------------------------------------------------------------
    // CONVENIENCE METHODS

    /**
     * Convenience method which determines whether or not the node subscribes to a topic
     *
     * @param topic the topic to check
     * @return true if the node subscribes to the topic, false if not
     */
    public boolean subscribesTo(int topic);


    /**
     * Necessary in order to add topic attributes to edges
     *
     * @return topic neighbors of this node
     */
    Multimap<String, String> reportTopicNeighbors();

    public void clearPublications();

}

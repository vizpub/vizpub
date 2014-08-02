package no.uio.ifi.vizpub.collector.gexf;

import com.esotericsoftware.minlog.Log;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import no.uio.ifi.vizpub.reports.NodeData;
import no.uio.ifi.vizpub.reports.PubMessage;
import no.uio.ifi.vizpub.reports.Report;
import no.uio.ifi.vizpub.utils.LogCategory;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;


/**
 * Class used to represent and visualize graph attributes.
 *
 * As Gephi only support labels on nodes and edges, visualizing graph attributes such
 * as hit ratio and path length is solved by creating designated nodes for each reports property
 * and displaying the value as a label.
 *
 * @author Nils Peder Korsveien
 */
public class GraphAttributes {

    private GraphAttributes() {
    }

    public static double getHitRatio(Report overlay) {
        checkNotNull(overlay);

        // fill a map of subscribers per topic
        Multimap<String, String> topicSubscribers = HashMultimap.create();
        for (NodeData nodeData : overlay.getNodes().values()) {
            for (String topicId : nodeData.getTopics()) {
                topicSubscribers.put(topicId, nodeData.getId());
            }
        }
//        BigDecimal hitRatio = new BigDecimal(0)
//                .setScale(3, BigDecimal.ROUND_HALF_UP);

        double hitRatio = 0.0;

        if (overlay.getPublications().isEmpty()) {
            Log.info(LogCategory.COLLECTOR, "No publications found for interval " + overlay.getIntervalCount());
        }

        for (PubMessage pubMessage : overlay.getPublications().values()) {
            Collection<String> subscribers = topicSubscribers.get(pubMessage.getTopicId());

            int subscriberCount = subscribers.size();
            int hitCount = 0;
            String topicId = pubMessage.getTopicId();

            for (String subscriberId : subscribers) {
                NodeData subscriber = overlay.getNode(subscriberId);
                if (subscriber.receivedPublication(pubMessage.getMsgId())) {
                    hitCount++;
                }
            }

            /**
             * Check if the publisher sends the publication to itself. If not increment
             * the received count anyway as the publisher is counted as one of
             * the subscribers.
             */
            String originalSenderId = pubMessage.getOriginalSenderId();
            String msgId = pubMessage.getMsgId();
            NodeData publisher = overlay.getNode(originalSenderId);
            if (publisher.subscribesTo(topicId) && !publisher.receivedPublication(msgId)) {
                hitCount++;
            }

//            hitRatio = hitRatio.add(new BigDecimal((double) hitCount / subscriberCount)
//                    .setScale(6, BigDecimal.ROUND_HALF_UP));

            hitRatio += (double) hitCount / subscriberCount;
        }

        final int publicationCount = overlay.getPublications().size();

        if (publicationCount != 0) {
              hitRatio = hitRatio / publicationCount;
//            hitRatio = Math.round((hitRatio / publicationCount) * 100.0) / 100.0;
//            BigDecimal divisor = new BigDecimal(publicationCount);
//            hitRatio = hitRatio.divide(divisor, 6, BigDecimal.ROUND_HALF_UP);

        }
//        return hitRatio.doubleValue();
        return hitRatio;
    }

    public static double getAveragePathLength(Report overlay) {
        Map<String, Integer> maxPathLengths = new HashMap<>();
        for (NodeData nodeData : overlay.getNodes().values()) {
            for (PubMessage pubMessage : nodeData.getPublicationMsgsReceived().values()) {
                int hopCount = pubMessage.getHopCount();
                Integer maxHopCount = maxPathLengths.get(pubMessage.getMsgId());

                if (maxHopCount == null) {
                    maxPathLengths.put(pubMessage.getMsgId(), hopCount);
                } else if (maxHopCount <= hopCount) {
                    maxPathLengths.put(pubMessage.getMsgId(), hopCount);
                }
            }
        }
        int totalHopCount = 0;
        for (Integer hopCount : maxPathLengths.values()) {
            totalHopCount += hopCount;
        }
        return (double) totalHopCount / maxPathLengths.size();
    }

    public static int getMaxPathLength(Report overlay) {
        int max = 0;
        for (NodeData nodeData : overlay.getNodes().values()) {
            for (PubMessage pubMessage : nodeData.getPublicationMsgsReceived().values()) {
                if (max < pubMessage.getHopCount()) {
                    max = pubMessage.getHopCount();
                }
            }
        }
        return max;
    }

    public static int getMinPathLength(Report overlay) {
        int min = 0;
        for (NodeData nodeData : overlay.getNodes().values()) {
            for (PubMessage pubMessage : nodeData.getPublicationMsgsReceived().values()) {
                if (min > pubMessage.getHopCount()) {
                    min = pubMessage.getHopCount();
                }
            }
        }
        return min;
    }
}

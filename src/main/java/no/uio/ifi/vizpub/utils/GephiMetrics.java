package no.uio.ifi.vizpub.utils;

import org.gephi.data.attributes.api.AttributeModel;
import org.gephi.graph.api.GraphModel;
import org.gephi.graph.api.Node;
import org.gephi.statistics.plugin.ClusteringCoefficient;
import org.gephi.statistics.plugin.Degree;
import org.gephi.statistics.plugin.GraphDistance;

/**
 * @author Nils Peder Korsveien
 */
public class GephiMetrics {

    public static double getAverageDegree(GraphModel graphModel, AttributeModel attributeModel) {
        return getAverageDegree(graphModel, attributeModel, Degree.DEGREE);
    }

    public static double getAverageInDegree(GraphModel graphModel, AttributeModel attributeModel) {
        return getAverageDegree(graphModel, attributeModel, Degree.INDEGREE);
    }

    public static double getAverageOutDegree(GraphModel graphModel, AttributeModel attributeModel) {
        return getAverageDegree(graphModel, attributeModel, Degree.OUTDEGREE);
    }

    /**
     * Measures how often a node appears on the shortest paths between nodes in the network
     */
    public static double getAverageBetweenessCentrality(GraphModel graphModel, AttributeModel attributeModel) {
        return getAverageGraphDistance(graphModel, attributeModel, GraphDistance.BETWEENNESS);
    }

    /**
     * The average distance from a given starting node to all other nodes in the network
     */
    public static double getAverageClosenessCentrality(GraphModel graphModel, AttributeModel attributeModel) {
        return getAverageGraphDistance(graphModel, attributeModel, GraphDistance.CLOSENESS);
    }

    /**
     * The distance from a given starting node to the farthest node from it in the network
     */
    public static double getAverageEccentricityCentrality(GraphModel graphModel, AttributeModel attributeModel) {
        return getAverageGraphDistance(graphModel, attributeModel, GraphDistance.ECCENTRICITY);
    }

    public static double getDiameter(GraphModel graphModel, AttributeModel attributeModel) {
        return getGraphDistance(graphModel, attributeModel).getDiameter();
    }

    public static double getAveragePathLength(GraphModel graphModel, AttributeModel attributeModel) {
        return getGraphDistance(graphModel, attributeModel).getPathLength();
    }

    /**
     * Indicates to which degree nodes in the network tend to cluster together
     */
    public  static double getAverageClusteringCoefficient(GraphModel graphModel, AttributeModel attributeModel) {
        ClusteringCoefficient clusteringCoefficient = new ClusteringCoefficient();
        clusteringCoefficient.execute(graphModel, attributeModel);

        double sum = 0;
        for (Node node : graphModel.getGraph().getNodes()) {
            sum += (double)node.getAttributes().getValue(ClusteringCoefficient.CLUSTERING_COEFF);
        }
        return sum / graphModel.getGraph().getNodeCount();
    }

    /* Private methods */

    private static double getAverageGraphDistance(GraphModel graphModel, AttributeModel attributeModel, String distanceType) {
        GraphDistance graphDistance = new GraphDistance();
        graphDistance.setDirected(true);
        graphDistance.setNormalized(true);
        graphDistance.execute(graphModel, attributeModel);

        double sum = 0;
        for (Node node : graphModel.getGraph().getNodes()) {
            sum += (double)node.getAttributes().getValue(distanceType);
        }
        return sum / graphModel.getGraph().getNodeCount();
    }

    private static double getAverageDegree(GraphModel graphModel, AttributeModel attributeModel, String degreeType) {
        Degree degree = new Degree();
        degree.execute(graphModel, attributeModel);
        int sum = 0;
        for (Node node : graphModel.getGraph().getNodes()) {
            sum += (int)node.getAttributes().getValue(degreeType);
        }
        return sum / graphModel.getGraph().getNodeCount();
    }

    private static GraphDistance getGraphDistance(GraphModel graphModel, AttributeModel attributeModel) {
        GraphDistance graphDistance = new GraphDistance();
        graphDistance.execute(graphModel, attributeModel);
        return graphDistance;
    }

}

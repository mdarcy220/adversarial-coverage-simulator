import java.util.ArrayList;
import java.util.List;

public class GraphNode extends Node {
	List<GraphEdge> edges = new ArrayList<GraphEdge>();
	
	public GraphNode(List<GraphEdge> edges, NodeType nodeType) {
		
	}
}

/**
 * Represents a directed edge in a graph.
 * @author Mike D'Arcy
 *
 */
class GraphEdge {
	double cost = 0.0;
	GraphNode node;

	public GraphEdge(double cost, GraphNode node) {
		this.cost = cost;
		this.node = node;
	}
}
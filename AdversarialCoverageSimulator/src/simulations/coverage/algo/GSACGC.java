package simulations.coverage.algo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import adsim.Algorithm;
import adsim.NodeType;
import gridenv.Coordinate;
import gridenv.GridActuator;
import gridenv.GridNode;
import gridenv.GridSensor;

/**
 * A coverage algorithm that uses the GSAC coverage path algorithm created by Roi Yehoshua
 * and Noa Agmon.
 * 
 * @author Mike D'Arcy
 *
 */
public class GSACGC implements Algorithm {

	GridSensor sensor;
	GridActuator actuator;

	private List<GridNode> coveragePath;
	private int stepNum = 0;


	public GSACGC(GridSensor sensor, GridActuator actuator) {
		this.sensor = sensor;
		this.actuator = actuator;
	}


	@Override
	public void init() {
		this.stepNum = 0;
		this.coveragePath = createGSACCoveragePath(createGraph(), this.sensor.getCurrentNode());
	}


	@Override
	public void step() {
		// Just follow the coverage path
		Coordinate curLoc = this.sensor.getLocation();
		int nextX;
		int nextY;
		if (this.stepNum < this.coveragePath.size()) {
			nextX = this.coveragePath.get(this.stepNum).getX();
			nextY = this.coveragePath.get(this.stepNum).getY();
			this.stepNum++;
		} else {
			this.actuator.coverCurrentNode();
			return;
		}
		int xdir = (int) Math.signum(nextX - curLoc.x);
		int ydir = (int) Math.signum(nextY - curLoc.y);

		if (xdir == 1 && ydir == 0) {
			this.actuator.moveRight();
		} else if (xdir == -1 && ydir == 0) {
			this.actuator.moveLeft();
		} else if (xdir == 0 && ydir == 1) {
			this.actuator.moveUp();
		} else if (xdir == 0 && ydir == -1) {
			this.actuator.moveDown();
		} else {
			this.actuator.coverCurrentNode();
		}
	}


	private List<GridNode> createGSACCoveragePath(GridNodeGraph graph, GridNode tmpStartNode) {
		GridNode startNode = tmpStartNode;
		for (GridNode node : graph.getAllNodes()) {
			if (node.getX() == startNode.getX() && node.getY() == startNode.getY()) {
				startNode = node;
			}
		}
		List<GridNode> path = new ArrayList<>();
		path.add(startNode);
		Set<GridNode> unvisited = new HashSet<>();
		unvisited.addAll(graph.getAllNodes());
		Iterator<GridNode> iter = unvisited.iterator();
		while (iter.hasNext()) {
			GridNode node = iter.next();
			if (node.getNodeType() == NodeType.OBSTACLE || (node.getX() == startNode.getX() && node.getY() == startNode.getY())) {
				iter.remove();
			}
		}
		unvisited.remove(startNode);
		GridNode curNode = startNode;

		while (!unvisited.isEmpty()) {
			// Find lowest-cost node
			DijkstraGraph dj = new DijkstraGraph(graph, curNode);
			double minCost = Double.POSITIVE_INFINITY;
			GridNode minCostNode = null;
			iter = unvisited.iterator();
			while (iter.hasNext()) {
				GridNode node = iter.next();
				double costToNode = dj.getCostToNode(node);
				if (costToNode < minCost || (costToNode == minCost && checkTiebreaker(node, minCostNode))) {
					minCost = costToNode;
					minCostNode = node;
				} else if (costToNode == Double.POSITIVE_INFINITY) {
					// Node is unreachable
					iter.remove();
				}
			}
			if (minCostNode == null) {
				continue;
			}
			List<GridNode> curPath = dj.getPathToNode(minCostNode);
			unvisited.removeAll(curPath);
			// Don't include the start node, or else it will be
			// added twice
			path.addAll(curPath);

			curNode = path.get(path.size() - 1);


		}

		return path;
	}


	private GridNodeGraph createGraph() {
		GridNodeGraph graph = new GridNodeGraph();
		// Create a temporary grid to avoid duplicating nodes
		GridNode[][] grid = new GridNode[this.sensor.getGridWidth()][this.sensor.getGridHeight()];
		for (int x = 0; x < this.sensor.getGridWidth(); x++) {
			for (int y = 0; y < this.sensor.getGridHeight(); y++) {
				grid[x][y] = this.sensor.getNodeAt(x, y);
				if (0.0 < grid[x][y].getDangerProb()) {
					grid[x][y].setCost((this.sensor.getGridWidth() * this.sensor.getGridHeight()) * grid[x][y].getDangerProb());
				} else {
					grid[x][y].setCost(1.0 / (this.sensor.getGridWidth() * this.sensor.getGridHeight()));
				}
				if (grid[x][y].getNodeType() == NodeType.OBSTACLE) {
					grid[x][y].setCost(Double.POSITIVE_INFINITY);
				}
			}
		}


		for (int x = 0; x < grid.length; x++) {
			for (int y = 0; y < grid[0].length; y++) {
				graph.addNode(grid[x][y]);
				GridNode adjNode;
				if (this.sensor.isOnGrid(x - 1, y)) {
					adjNode = grid[x - 1][y];
					graph.addEdge(grid[x][y], adjNode);
				}

				if (this.sensor.isOnGrid(x + 1, y)) {
					adjNode = grid[x + 1][y];
					graph.addEdge(grid[x][y], adjNode);
				}

				if (this.sensor.isOnGrid(x, y - 1)) {
					adjNode = grid[x][y - 1];
					graph.addEdge(grid[x][y], adjNode);
				}

				if (this.sensor.isOnGrid(x, y + 1)) {
					adjNode = grid[x][y + 1];
					graph.addEdge(grid[x][y], adjNode);
				}

			}
		}
		return graph;
	}


	private boolean checkTiebreaker(GridNode node, GridNode minDistNode) {
		int nodeSum = node.getX() + node.getY();
		int minDistNodeSum = minDistNode.getX() + minDistNode.getY();

		if (nodeSum < minDistNodeSum) {
			return true;
		} else if (nodeSum == minDistNodeSum) {
			return node.getY() < minDistNode.getY();
		}
		return false;
	}


	@Override
	public void reloadSettings() {
		this.sensor.reloadSettings();
		this.actuator.reloadSettings();
	}


}


class DijkstraGraph {
	Map<GridNode, GridNode> prevNodes = new HashMap<>();
	Map<GridNode, Double> costs = new HashMap<>();
	private Set<GridNode> unvisited = new HashSet<>();
	GridNode start;
	GridNode target = null;
	GridNodeGraph graph;


	public DijkstraGraph(GridNodeGraph graph, GridNode start) {
		this(graph, start, null);
	}


	public DijkstraGraph(GridNodeGraph graph, GridNode start, GridNode target) {
		this.graph = graph;
		this.unvisited.addAll(graph.getAllNodes());
		this.start = start;
		this.target = target;
		runDijkstra();
	}


	public List<GridNode> getPathToNode(GridNode target) {
		List<GridNode> path = new ArrayList<>();

		for (GridNode curNode = target; this.prevNodes.get(curNode) != null; curNode = this.prevNodes.get(curNode)) {
			path.add(curNode);
		}

		java.util.Collections.reverse(path);
		return path;
	}


	public double getCostToNode(GridNode target) {
		return this.costs.get(target).doubleValue();
	}


	private void initNodes() {
		for (GridNode node : this.unvisited) {
			this.prevNodes.put(node, null);
			this.costs.put(node, new Double(Double.POSITIVE_INFINITY));
		}
		this.costs.put(this.start, new Double(0.0));
	}


	private void runDijkstra() {
		initNodes();
		GridNode curNode;
		while (!this.unvisited.isEmpty()) {

			curNode = getMinCostNodeFromUnvisitedSet();
			if (curNode == this.target) {
				return;
			}
			this.unvisited.remove(curNode);
			double curCost = this.costs.get(curNode).doubleValue();
			for (GridNode node : this.graph.getAdjacentNodes(curNode)) {
				double nodeCost = node.getCost();
				double tmpCost = curCost + nodeCost;
				if (tmpCost < this.costs.get(node).doubleValue()) {
					this.costs.put(node, new Double(tmpCost));
					this.prevNodes.put(node, curNode);
				}
			}
		}
	}


	private GridNode getMinCostNodeFromUnvisitedSet() {
		double minDist = Double.POSITIVE_INFINITY;
		GridNode minDistNode = null;
		for (GridNode node : this.unvisited) {
			double dist = this.costs.get(node).doubleValue();
			if (dist < minDist || minDistNode == null) {
				minDist = dist;
				minDistNode = node;
			}
		}
		return minDistNode;
	}
}


/**
 * A graph whoose nodes are grid nodes. This type of graph is generally used to represent
 * a grid.
 * 
 * @author Mike D'Arcy
 *
 */
class GridNodeGraph {
	Map<GridNode, Set<GridNode>> adjacencyMap = new HashMap<>();


	public GridNodeGraph() {

	}


	public void addNode(GridNode node) {
		if (!this.hasNode(node)) {
			this.adjacencyMap.put(node, new HashSet<GridNode>());
		}
	}


	public void addEdge(GridNode node1, GridNode node2) {
		this.adjacencyMap.get(node1).add(node2);
	}


	public Set<GridNode> getAdjacentNodes(GridNode node) {
		return this.adjacencyMap.get(node);
	}


	public Set<GridNode> getAllNodes() {
		return this.adjacencyMap.keySet();
	}


	public boolean hasNode(GridNode node) {
		return this.adjacencyMap.containsKey(node);
	}

}

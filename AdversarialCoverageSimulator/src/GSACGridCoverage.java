import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A coverage algorithm that uses the GSAC coverage path algorithm created by
 * Roi Yehoshua and Noa Agmon.
 * 
 * @author Mike D'Arcy
 *
 */
public class GSACGridCoverage extends CoverageAlgorithm {

	GridSensor sensor;
	GridActuator actuator;

	private List<GridNode> coveragePath;
	private int stepNum = 0;


	public GSACGridCoverage(GridSensor sensor, GridActuator actuator) {
		this.sensor = sensor;
		this.actuator = actuator;
		System.out.printf("Robot at (%d, %d)\n", sensor.getLocation().x, sensor.getLocation().y);
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


	@Override
	public void init() {
		coveragePath = createGSACCoveragePath(createGraph(), sensor.getCurrentNode());
	}


	@Override
	public void step() {
		// Just follow the coverage path
		Coordinate curLoc = sensor.getLocation();
		int nextX;
		int nextY;
		if (stepNum < this.coveragePath.size()) {
			nextX = this.coveragePath.get(stepNum).getX();
			nextY = this.coveragePath.get(stepNum).getY();
			stepNum++;
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


	private List<GridNode> createGSACCoveragePath(GridNodeGraph graph, GridNode startNode) {
		for(GridNode node : graph.getAllNodes()) {
			if(node.getX() == startNode.getX() && node.getY() == startNode.getY()) {
				startNode = node;
			}
		}
		List<GridNode> path = new ArrayList<GridNode>();
		path.add(startNode);
		Set<GridNode> unvisited = new HashSet<GridNode>();
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
		
		for(GridNode node : graph.getAllNodes()) {
			System.out.printf("(%d, %d) -> ", node.getX(), node.getY());
			for(GridNode node2 : graph.getAdjacentNodes(node)) {
				System.out.printf("(%d, %d), ", node2.getX(), node2.getY());
			}
			System.out.println();
		}

		while (!unvisited.isEmpty()) {
			// Find lowest-cost node
			DijkstraGraph dj = new DijkstraGraph(graph, curNode);
			double minCost = Double.POSITIVE_INFINITY;
			GridNode minCostNode = null;
			iter = unvisited.iterator();
			while (iter.hasNext()) {
				GridNode node = iter.next();
				double costToNode = dj.getCostToNode(node);
				if (costToNode < minCost) {
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
			//curPath.remove(0);
			path.addAll(curPath);
			
			curNode = path.get(path.size() - 1);
			
			
		}
		for (GridNode node : path) {
			System.out.printf("(%d, %d) -> \n", node.getX(), node.getY());
		}
		System.out.println("END");

		return path;
	}


	private double getPathCost(List<GridNode> path) {
		double cost = 0.0;
		for (int i = 1; i < path.size(); i++) {
			cost += path.get(i).getCost();
		}
		return cost;
	}


	private boolean allGraphNodesCovered(GridNodeGraph graph) {
		for (GridNode node : graph.getAllNodes()) {
			if (node.coverCount <= 0) {
				return false;
			}
		}
		return true;
	}


	private GridNodeGraph createGraph() {
		GridNodeGraph graph = new GridNodeGraph();
		// Create a temporary grid to avoid duplicating nodes
		GridNode[][] grid = new GridNode[sensor.getGridWidth()][sensor.getGridHeight()];
		for (int x = 0; x < sensor.getGridWidth(); x++) {
			for (int y = 0; y < sensor.getGridHeight(); y++) {
				grid[x][y] = sensor.getNodeAt(x, y);
				grid[x][y].setCost(0.0 < grid[x][y].getDangerProb() ? 1.0
						: 1.0 / ((double) grid.length));
				if (grid[x][y].getNodeType() == NodeType.OBSTACLE) {
					grid[x][y].setCost(Double.POSITIVE_INFINITY);
				}
			}
		}


		for (int x = 0; x < grid.length; x++) {
			for (int y = 0; y < grid[0].length; y++) {
				graph.addNode(grid[x][y]);
				GridNode adjNode;
				if (sensor.isOnGrid(x - 1, y)) {
					adjNode = grid[x - 1][y];
					graph.addEdge(grid[x][y], adjNode);
				}

				if (sensor.isOnGrid(x + 1, y)) {
					adjNode = grid[x + 1][y];
					graph.addEdge(grid[x][y], adjNode);
				}

				if (sensor.isOnGrid(x, y - 1)) {
					adjNode = grid[x][y - 1];
					graph.addEdge(grid[x][y], adjNode);
				}

				if (sensor.isOnGrid(x, y + 1)) {
					adjNode = grid[x][y + 1];
					graph.addEdge(grid[x][y], adjNode);
				}

			}
		}
		return graph;
	}


}


class DijkstraGraph {
	Map<GridNode, GridNode> prevNodes = new HashMap<GridNode, GridNode>();
	Map<GridNode, Double> costs = new HashMap<GridNode, Double>();
	private Set<GridNode> unvisited = new HashSet<GridNode>();
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
		List<GridNode> path = new ArrayList<GridNode>();

		for (GridNode curNode = target; this.prevNodes.get(curNode) != null; curNode = this.prevNodes
				.get(curNode)) {
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
			if(node.equals(this.start)) {
				System.out.printf("FOUND IT!%d %d\n", node.getX(), node.getY());
			}
		}
		System.out.printf("Starting at (%d, %d)\n", this.start.getX(), this.start.getY());
		this.costs.put(this.start, new Double(0.0));
	}


	private void runDijkstra() {
		initNodes();
		GridNode curNode;
		while (!this.unvisited.isEmpty()) {
			
			curNode = getMinCostNodeFromUnvisitedSet();
			if (curNode == target) {
				return;
			}
			this.unvisited.remove(curNode);
			double curCost = this.costs.get(curNode).doubleValue();
			for (GridNode node : graph.getAdjacentNodes(curNode)) {
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
			if (this.costs.get(node).doubleValue() < minDist || minDistNode == null) {
				minDistNode = node;
			}
		}
		return minDistNode;
	}
}


/**
 * A graph whoose nodes are grid nodes. This type of graph is generally used to
 * represent a grid.
 * 
 * @author Mike D'Arcy
 *
 */
class GridNodeGraph {
	Map<GridNode, Set<GridNode>> adjacencyMap = new HashMap<GridNode, Set<GridNode>>();


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

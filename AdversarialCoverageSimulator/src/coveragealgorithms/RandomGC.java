package coveragealgorithms;

import adversarialcoverage.GridActuator;
import adversarialcoverage.GridNode;
import adversarialcoverage.GridSensor;
import adversarialcoverage.Node;
import adversarialcoverage.NodeType;

/**
 * A coverage algorithm that covers the grid using an e-greedy policy, moving either
 * randomly or taking the action with highest immediate reward (i.e., looking only one
 * step into the future).
 * 
 * @author Mike D'Arcy
 *
 */
public class RandomGC extends GridCoverageAlgorithm {
	GridSensor sensor;
	GridActuator actuator;
	long stepNum = 0;


	public RandomGC(GridSensor sensor, GridActuator actuator) {
		this.sensor = sensor;
		this.actuator = actuator;
	}


	@Override
	public void init() {
		this.actuator.coverCurrentNode();
	}


	@Override
	public void step() {
		int bestDirection = 0;

		int minHeuristic = Integer.MAX_VALUE;
		if (isValidCoverNode(getNodeRelative(1, 0)) && heuristic(getNodeRelative(1, 0)) < minHeuristic) {
			minHeuristic = getNodeRelative(1, 0).getCoverCount();
			bestDirection = 0;
		}
		if (isValidCoverNode(getNodeRelative(0, 1)) && heuristic(getNodeRelative(0, 1)) < minHeuristic) {
			minHeuristic = getNodeRelative(0, 1).getCoverCount();
			bestDirection = 1;
		}
		if (isValidCoverNode(getNodeRelative(-1, 0)) && heuristic(getNodeRelative(-1, 0)) < minHeuristic) {
			minHeuristic = getNodeRelative(-1, 0).getCoverCount();
			bestDirection = 2;
		}
		if (isValidCoverNode(getNodeRelative(0, -1)) && heuristic(getNodeRelative(0, -1)) < minHeuristic) {
			minHeuristic = getNodeRelative(0, -1).getCoverCount();
			bestDirection = 3;
		}

		int direction = 0;
		double rnum = Math.random();

		if (rnum < 0) {
			direction = bestDirection;
		} else {
			direction = (int) (Math.random() * 4);
		}

		takeAction(direction);
		this.stepNum++;
	}


	private void takeAction(int actionNum) {
		if (actionNum == 0) {
			this.actuator.moveRight();
		} else if (actionNum == 1) {
			this.actuator.moveUp();
		} else if (actionNum == 2) {
			this.actuator.moveLeft();
		} else if (actionNum == 3) {
			this.actuator.moveDown();
		} else {
			this.actuator.coverCurrentNode();
		}
	}


	private int heuristic(Node n) {
		return n.getCoverCount();
	}


	private boolean isValidCoverNode(Node n) {
		return (n != null && n.getNodeType() != NodeType.OBSTACLE);
	}


	private GridNode getNodeRelative(int dx, int dy) {
		return this.sensor.getNodeAt(this.sensor.getLocation().x + dx, this.sensor.getLocation().y + dy);
	}

}

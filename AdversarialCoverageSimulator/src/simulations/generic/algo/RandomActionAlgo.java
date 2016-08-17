package simulations.generic.algo;

import adsim.Actuator;
import adsim.Algorithm;
import adsim.Node;
import adsim.NodeType;
import gridenv.GridNode;
import gridenv.GridSensor;

/**
 * A coverage algorithm that covers the grid using an e-greedy policy, moving either
 * randomly or taking the action with highest immediate reward (i.e., looking only one
 * step into the future).
 * 
 * @author Mike D'Arcy
 *
 */
public class RandomActionAlgo implements Algorithm {
	GridSensor sensor;
	Actuator actuator;
	long stepNum = 0;


	public RandomActionAlgo(GridSensor sensor, Actuator actuator) {
		this.sensor = sensor;
		this.actuator = actuator;
	}


	@Override
	public void init() {
		
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

		this.actuator.takeActionById(direction);
		this.stepNum++;
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


	@Override
	public void reloadSettings() {
		
	}

}

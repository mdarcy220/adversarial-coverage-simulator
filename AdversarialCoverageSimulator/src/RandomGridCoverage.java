
public class RandomGridCoverage extends CoverageAlgorithm {
	GridSensor sensor;
	GridActuator actuator;


	public RandomGridCoverage(GridSensor sensor, GridActuator actuator) {
		this.sensor = sensor;
		this.actuator = actuator;
	}


	@Override
	public void init() {
		actuator.coverCurrentNode();
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

		if (rnum < 1) {
			direction = bestDirection;
		} else {
			direction = (int) (Math.random() * 4);
		}

		switch (direction) {
		case 0:
			actuator.moveRight();
			break;
		case 1:
			actuator.moveUp();
			break;
		case 2:
			actuator.moveLeft();
			break;
		case 3:
			actuator.moveDown();
			break;
		default:
			actuator.coverCurrentNode();
		}
	}


	private int heuristic(Node n) {
		return n.getCoverCount();
	}


	private boolean isValidCoverNode(Node n) {
		return (n != null && n.getNodeType() != NodeType.OBSTACLE);
	}


	private GridNode getNodeRelative(int dx, int dy) {
		return sensor.getNodeAt(sensor.getLocation().x + dx, sensor.getLocation().y + dy);
	}

}

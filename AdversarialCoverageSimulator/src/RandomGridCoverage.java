
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
		int leastCoveredDirection = 0;

		int minCount = Integer.MAX_VALUE;
		if (getNodeRelative(1, 0) != null && getNodeRelative(1, 0).getCoverCount() < minCount) {
			minCount = getNodeRelative(1, 0).getCoverCount();
			leastCoveredDirection = 0;
		}
		if (getNodeRelative(0, 1) != null && getNodeRelative(0, 1).getCoverCount() < minCount) {
			minCount = getNodeRelative(0, 1).getCoverCount();
			leastCoveredDirection = 1;
		}
		if (getNodeRelative(-1, 0) != null && getNodeRelative(-1, 0).getCoverCount() < minCount) {
			minCount = getNodeRelative(-1, 0).getCoverCount();
			leastCoveredDirection = 2;
		}
		if (getNodeRelative(0, -1) != null && getNodeRelative(0, -1).getCoverCount() < minCount) {
			minCount = getNodeRelative(0, -1).getCoverCount();
			leastCoveredDirection = 3;
		}

		int direction = 0;
		double rnum = Math.random();

		if (rnum < 0.5) {
			direction = leastCoveredDirection;
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

	private GridNode getNodeRelative(int dx, int dy) {
		return sensor.getNodeAt(sensor.getLocation().x + dx, sensor.getLocation().y + dy);
	}

}

/**
 * A sensor for grids. It can detect the entire grid (including danger
 * level, the number of times each space was covered, etc) and the robot's
 * location
 * 
 * @author Mike D'Arcy
 *
 */
public class GridSensor extends Sensor {
	GridRobot robot;
	GridEnvironment env;

	/**
	 * Constructs a sensor for the given environment and robot
	 * 
	 * @param env
	 *            the environment that this sensor will get observations from
	 * @param robot
	 *            the robot that has this sensor
	 */
	public GridSensor(GridEnvironment env, GridRobot robot) {
		this.env = env;
		this.robot = robot;
	}

	/**
	 * Gets the location of the robot to which this sensor is attached
	 * 
	 * @return the robot's current location as a {@code Coordinate}
	 */
	public Coordinate getLocation() {
		return this.robot.getLocation().clone();
	}

	/**
	 * Returns the grid node located at the given coordinates
	 * 
	 * @param x
	 * @param y
	 * @return a {@code GridNode}
	 */
	public GridNode getNodeAt(int x, int y) {
		GridNode node = env.getGridNode(x, y);
		if(node != null) {
			return node.clone();
		} else {
			return null;
		}
	}
	
	
	public int getGridWidth() {
		return env.getWidth();
	}
	
	public int getGridHeight() {
		return env.getHeight();
	}
	
	public boolean isOnGrid(int x, int y) {
		return env.isOnGrid(x, y);
	}
}

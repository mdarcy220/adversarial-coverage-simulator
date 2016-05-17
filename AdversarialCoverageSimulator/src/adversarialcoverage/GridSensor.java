package adversarialcoverage;

/**
 * A sensor for grids. It can detect the entire grid (including danger level, the number
 * of times each space was covered, etc) and the robot's location
 * 
 * @author Mike D'Arcy
 *
 */
public class GridSensor {
	GridRobot robot;
	GridEnvironment env;


	/**
	 * Constructs a sensor for the given environment and robot
	 * 
	 * @param env
	 *                the environment that this sensor will get observations from
	 * @param robot
	 *                the robot that has this sensor
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
		return new Coordinate(this.robot.getLocation().x, this.robot.getLocation().y);
	}


	/**
	 * @return the x coordinate of the robot
	 */
	public int getX() {
		return this.robot.getLocation().x;
	}


	/**
	 * 
	 * @return the y coordinate of the robot
	 */
	public int getY() {
		return this.robot.getLocation().y;
	}


	public boolean nodeExists(int x, int y) {
		return this.env.isOnGrid(x, y);
	}


	/**
	 * Returns the grid node located at the given coordinates
	 * 
	 * @param x
	 * @param y
	 * @return a {@code GridNode}
	 */
	public GridNode getNodeAt(int x, int y) {
		GridNode node = this.env.getGridNode(x, y);
		if (node != null) {
			return node.clone();
		}
		return null;
	}


	/**
	 * Get the danger level of the grid node at the given coordinates
	 * 
	 * @param x
	 * @param y
	 * @return
	 */
	public double getDangerLevelAt(int x, int y) {
		return this.env.getGridNode(x, y).getDangerProb();
	}


	/**
	 * Get the cover count of the grid node at the given coordinates
	 * 
	 * @param x
	 * @param y
	 * @return
	 */
	public int getCoverCountAt(int x, int y) {
		return this.env.getGridNode(x, y).getCoverCount();
	}


	public boolean robotIsBroken() {
		return this.robot.isBroken();
	}


	/**
	 * Get the width of the environment
	 * 
	 * @return
	 */
	public int getGridWidth() {
		return this.env.getWidth();
	}


	/**
	 * Get the height of the environment
	 * 
	 * @return
	 */
	public int getGridHeight() {
		return this.env.getHeight();
	}


	/**
	 * Check if the given coordinates are within the environment
	 * 
	 * @param x
	 * @param y
	 * @return
	 */
	public boolean isOnGrid(int x, int y) {
		return this.env.isOnGrid(x, y);
	}


	/**
	 * Returns the node that the robot is currently at
	 * 
	 * @return
	 */
	public GridNode getCurrentNode() {
		return this.getNodeAt(this.robot.getLocation().x, this.robot.getLocation().y);
	}


	public void reloadSettings() {
		return;
	}


	public boolean isObstacle(int x, int y) {
		return this.env.getGridNode(x, y).getNodeType() == NodeType.OBSTACLE;
	}
}

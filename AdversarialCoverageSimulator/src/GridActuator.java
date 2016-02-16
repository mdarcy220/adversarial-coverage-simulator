
public class GridActuator extends Actuator {
	/**
	 * The environment in which this actuator exists
	 */
	private GridEnvironment env;
	/**
	 * The robot to which this actuator is attached.
	 */
	private GridRobot robot;

	/**
	 * Constructs an actuator for the given environment and robot
	 * 
	 * @param env
	 *            the environment that this actuator will affect
	 * @param robot
	 *            the robot that controls this actuator
	 */
	public GridActuator(GridEnvironment env, GridRobot robot) {
		this.env = env;
		this.robot = robot;
	}

	/**
	 * Move the robot 1 cell to the right on the grid
	 */
	public void moveRight() {
		Coordinate newLoc = new Coordinate(this.robot.getLocation().x + 1, this.robot.getLocation().y);
		moveTo(newLoc);
	}

	/**
	 * Move the robot 1 cell to the left on the grid
	 */
	public void moveLeft() {
		Coordinate newLoc = new Coordinate(this.robot.getLocation().x - 1, this.robot.getLocation().y);
		moveTo(newLoc);
	}

	/**
	 * Move the robot 1 cell upward (North) on the grid
	 */
	public void moveUp() {
		Coordinate newLoc = new Coordinate(this.robot.getLocation().x, this.robot.getLocation().y + 1);
		moveTo(newLoc);
	}

	/**
	 * Move the robot 1 cell downward (South) on the grid
	 */
	public void moveDown() {
		Coordinate newLoc = new Coordinate(this.robot.getLocation().x, this.robot.getLocation().y - 1);
		moveTo(newLoc);
	}

	private void moveTo(Coordinate newLoc) {
		if(robot.isBroken()) {
			return;
		}
		
		// Move, if possible
		if (env.isOnGrid(newLoc.x, newLoc.y)
				&& env.getGridNode(newLoc.x, newLoc.y).getNodeType() != NodeType.OBSTACLE) {
			this.robot.setLocation(newLoc.x, newLoc.y);
		}

		coverCurrentNode();
	}

	/**
	 * Don't move, just cover the current node again.
	 */
	public void coverCurrentNode() {
		double rand = Math.random();
		
		if(rand < env.getGridNode(this.robot.getLocation().x, this.robot.getLocation().y).getDangerProb()) {
			//env.getRobotById(this.robot.getId()).setBroken(true);			
		}
		env.getGridNode(this.robot.getLocation().x, this.robot.getLocation().y).incrementCoverCount();
	}
}

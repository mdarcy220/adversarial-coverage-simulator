package adversarialcoverage;
public class GridActuator extends Actuator {
	/**
	 * The environment in which this actuator exists
	 */
	private GridEnvironment env;
	/**
	 * The robot to which this actuator is attached.
	 */
	private GridRobot robot;
	private double lastReward = 0.0;


	/**
	 * Constructs an actuator for the given environment and robot
	 * 
	 * @param env
	 *                the environment that this actuator will affect
	 * @param robot
	 *                the robot that controls this actuator
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
		this.lastReward = 0.0;
		if (this.robot.isBroken()) {
			return;
		}

		// Move, if possible
		if (this.env.isOnGrid(newLoc.x, newLoc.y)
				&& this.env.getGridNode(newLoc.x, newLoc.y).getNodeType() != NodeType.OBSTACLE
				&& this.env.getRobotsByLocation(newLoc.x, newLoc.y).size() == 0) {
			this.robot.setLocation(newLoc.x, newLoc.y);
		}

		coverCurrentNode();
	}


	/**
	 * Don't move, just cover the current node again.
	 */
	public void coverCurrentNode() {
		double rand = Math.random();

		if (rand < this.env.getGridNode(this.robot.getLocation().x, this.robot.getLocation().y).getDangerProb()
				&& AdversarialCoverage.settings.getBooleanProperty("robots.breakable")) {
			this.env.getRobotById(this.robot.getId()).setBroken(true);
			this.lastReward = -2.0;
			return;
		}
		
		int coverCount = this.env.getGridNode(this.robot.getLocation().x, this.robot.getLocation().y).getCoverCount();
		if(coverCount == 0) {
			this.env.squaresLeft--;
		}
		this.lastReward = coverCount < 1 ? 1.0 : -0.1;
		//this.lastReward -= this.env.getGridNode(this.robot.getLocation().x, this.robot.getLocation().y).getDangerProb();
		if(this.env.isCovered()) {
			this.lastReward = 4.0;
		}
		//this.lastReward -= this.env.getGridNode(this.robot.getLocation().x, this.robot.getLocation().y).getDangerProb()*10;
		this.env.getGridNode(this.robot.getLocation().x, this.robot.getLocation().y).incrementCoverCount();
		AdversarialCoverage.stats.updateCellCovered(this.robot);
	}
	
	/**
	 * Get the reward value from the most recent action (usually useful for MDP-based coverage algorithms)
	 * @return
	 */
	public double getLastReward() {
		return this.lastReward;
	}
}
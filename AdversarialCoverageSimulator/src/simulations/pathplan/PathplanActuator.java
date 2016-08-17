package simulations.pathplan;

import adsim.NodeType;
import adsim.SimulatorMain;
import adsim.SimulatorSettings;
import deeplearning.DQLActuator;
import gridenv.Coordinate;
import gridenv.GridEnvironment;
import gridenv.GridRobot;

public class PathplanActuator implements DQLActuator {
	private PathplanSimulation simulation;
	/**
	 * The environment in which this actuator exists
	 */
	private GridEnvironment env;
	/**
	 * The robot to which this actuator is attached.
	 */
	private GridRobot robot;
	private double lastReward = 0.0;
	private int lastActionId = -1;
	private double REACH_GOAL_REWARD = SimulatorMain.settings.getDouble("pathplan.dql.reward.reach_goal");
	private double DEATH_REWARD = SimulatorMain.settings.getDouble("pathplan.dql.reward.death");
	private boolean ROBOTS_BREAKABLE = SimulatorMain.settings.getBoolean("robots.breakable");


	/**
	 * Constructs an actuator for the given environment and robot
	 * 
	 * @param env
	 *                the environment that this actuator will affect
	 * @param robot
	 *                the robot that controls this actuator
	 */
	public PathplanActuator(GridEnvironment env, GridRobot robot, PathplanSimulation covSim) {
		this.env = env;
		this.robot = robot;
		this.simulation = covSim;
		this.registerSettings();
	}


	private void registerSettings() {
		SimulatorSettings settings = SimulatorMain.settings;
		String settingName = "";

		settingName = "pathplan.dql.reward.reach_goal";
		if (!settings.hasProperty(settingName)) {
			settings.setDouble(settingName, 1.0);
		}

		settingName = "pathplan.dql.reward.death";
		if (!settings.hasProperty(settingName)) {
			settings.setDouble(settingName, -1.0);
		}
	}


	/**
	 * Move the robot 1 cell to the right on the grid
	 */
	public void moveRight() {
		Coordinate newLoc = new Coordinate(this.robot.getLocation().x + 1, this.robot.getLocation().y);
		moveTo(newLoc);
		this.lastActionId = 0;
	}


	/**
	 * Move the robot 1 cell to the left on the grid
	 */
	public void moveLeft() {
		Coordinate newLoc = new Coordinate(this.robot.getLocation().x - 1, this.robot.getLocation().y);
		moveTo(newLoc);
		this.lastActionId = 2;
	}


	/**
	 * Move the robot 1 cell upward (North) on the grid
	 */
	public void moveUp() {
		Coordinate newLoc = new Coordinate(this.robot.getLocation().x, this.robot.getLocation().y + 1);
		moveTo(newLoc);
		this.lastActionId = 1;
	}


	/**
	 * Move the robot 1 cell downward (South) on the grid
	 */
	public void moveDown() {
		Coordinate newLoc = new Coordinate(this.robot.getLocation().x, this.robot.getLocation().y - 1);
		moveTo(newLoc);
		this.lastActionId = 3;
	}


	private void moveTo(Coordinate newLoc) {
		this.lastReward = 0.0;
		if (this.robot.isBroken()) {
			return;
		}

		// Move, if possible
		if (this.env.isOnGrid(newLoc.x, newLoc.y) && this.env.getGridNode(newLoc.x, newLoc.y).getNodeType() != NodeType.OBSTACLE
				&& this.env.getRobotsByLocation(newLoc.x, newLoc.y).size() == 0) {
			this.robot.setLocation(newLoc.x, newLoc.y);
		}

		this.processCoveringCurrentNode();
	}


	/**
	 * Don't move, just cover the current node again.
	 */
	public void coverCurrentNode() {
		this.processCoveringCurrentNode();
		this.lastActionId = 4;
	}


	private void processCoveringCurrentNode() {
		double rand = Math.random();
		boolean isThreat = rand < this.env.getGridNode(this.robot.getLocation().x, this.robot.getLocation().y).getDangerProb()
				&& this.ROBOTS_BREAKABLE;

		this.lastReward = this.calcMoveReward(isThreat);

		SimulatorMain.getStats().updateCellCovered(this.robot);
		this.env.getGridNode(this.robot.getLocation().x, this.robot.getLocation().y).incrementCoverCount();
		if (isThreat) {
			this.env.getRobotById(this.robot.getId()).setBroken(true);
		}
	}


	/**
	 * Gets the reward for covering the given cell. This method must be called BEFORE
	 * the cover count is incremented.
	 * 
	 * @param oldCoverCount
	 *                the cover count the cell had before being covered by the robot
	 * @param isThreat
	 *                whether the cell will kill the robot
	 * @return the reward
	 */
	private double calcMoveReward(boolean isThreat) {
		double reward = 0.0;
		if (isThreat) {
			reward = this.DEATH_REWARD;
		} else if (this.robot.getLocation().x == this.simulation.getGoalX() && this.robot.getLocation().y == this.simulation.getGoalY()) {
			reward = this.REACH_GOAL_REWARD;
		}
		return reward;
	}


	@Override
	public void takeActionById(int actionNum) {
		if (actionNum == 0) {
			this.moveRight();
		} else if (actionNum == 1) {
			this.moveUp();
		} else if (actionNum == 2) {
			this.moveLeft();
		} else if (actionNum == 3) {
			this.moveDown();
		} else if (actionNum == 4) {
			this.coverCurrentNode();
		} else {
			this.coverCurrentNode();
		}
	}


	@Override
	public int getLastActionId() {
		return this.lastActionId;
	}


	/**
	 * Get the reward value from the most recent action (usually useful for MDP-based
	 * coverage algorithms)
	 * 
	 * @return
	 */
	@Override
	public double getLastReward() {
		return this.lastReward;
	}


	@Override
	public void reloadSettings() {
		this.REACH_GOAL_REWARD = SimulatorMain.settings.getDouble("deepql.reward.cover_again");
		this.DEATH_REWARD = SimulatorMain.settings.getDouble("deepql.reward.death");
		this.ROBOTS_BREAKABLE = SimulatorMain.settings.getBoolean("robots.breakable");
	}
}

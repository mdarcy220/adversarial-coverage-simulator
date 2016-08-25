package simulations.coverage;

import adsim.SimulatorMain;
import deeplearning.DQLActuator;
import gridenv.Coordinate;
import gridenv.GridEnvironment;
import gridenv.GridRobot;
import gridenv.NodeType;

public class CoverageActuator implements DQLActuator {
	private CoverageSimulation simulation;
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
	private double COVER_UNIQUE_REWARD = SimulatorMain.settings.getDouble("deepql.reward.cover_unique");
	private double COVER_AGAIN_REWARD = SimulatorMain.settings.getDouble("deepql.reward.cover_again");
	private double DEATH_REWARD = SimulatorMain.settings.getDouble("deepql.reward.death");
	private double FULL_COVERAGE_REWARD = SimulatorMain.settings.getDouble("deepql.reward.full_coverage");
	private boolean ROBOTS_BREAKABLE = SimulatorMain.settings.getBoolean("robots.breakable");


	/**
	 * Constructs an actuator for the given environment and robot
	 * 
	 * @param env
	 *                the environment that this actuator will affect
	 * @param robot
	 *                the robot that controls this actuator
	 */
	public CoverageActuator(GridEnvironment env, GridRobot robot, CoverageSimulation covSim) {
		this.env = env;
		this.robot = robot;
		this.simulation = covSim;
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
		int coverCount = this.env.getGridNode(this.robot.getLocation().x, this.robot.getLocation().y).getCoverCount();

		this.lastReward = this.getCellCoverageReward(coverCount, isThreat);

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
	private double getCellCoverageReward(int oldCoverCount, boolean isThreat) {
		double reward = 0.0;
		if (oldCoverCount == 0) {
			this.simulation.onNewCellCovered();
		}

		if (isThreat) {
			reward = this.DEATH_REWARD;

		} else {
			reward = oldCoverCount < 1 ? this.COVER_UNIQUE_REWARD : this.COVER_AGAIN_REWARD;

			if (this.simulation.isTerminalState()) {
				reward = this.FULL_COVERAGE_REWARD;
			}
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


	@Override
	public double getLastReward() {
		return this.lastReward;
	}


	@Override
	public void reloadSettings() {
		this.COVER_UNIQUE_REWARD = SimulatorMain.settings.getDouble("deepql.reward.cover_unique");
		this.COVER_AGAIN_REWARD = SimulatorMain.settings.getDouble("deepql.reward.cover_again");
		this.DEATH_REWARD = SimulatorMain.settings.getDouble("deepql.reward.death");
		this.FULL_COVERAGE_REWARD = SimulatorMain.settings.getDouble("deepql.reward.full_coverage");
		this.ROBOTS_BREAKABLE = SimulatorMain.settings.getBoolean("robots.breakable");
	}


	@Override
	public int getNumActions() {
		return 5;
	}
}

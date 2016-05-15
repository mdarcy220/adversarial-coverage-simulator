package adversarialcoverage;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;

public class GridEnvironment {
	public GridNode[][] grid;
	public Dimension gridSize = new Dimension();
	public List<GridRobot> robots;
	private int stepCount = 0;
	public int squaresLeft = 0;
	private boolean RANDOMIZE_ROBOT_LOCATION_ON_INIT = AdversarialCoverage.settings.getBooleanProperty("autorun.randomize_robot_start");
	private boolean CLEAR_ADJACENT_CELLS_ON_INIT = AdversarialCoverage.settings.getBooleanProperty("env.clear_adjacent_cells_on_init");
	private int MAX_STEPS_PER_RUN = AdversarialCoverage.settings.getIntProperty("autorun.max_steps_per_run");


	public GridEnvironment(Dimension gridSize) {
		this.gridSize.width = gridSize.width;
		this.gridSize.height = gridSize.height;

		// Set up the robot list
		this.robots = new ArrayList<>();

		this.grid = new GridNode[gridSize.width][gridSize.height];
		for (int x = 0; x < gridSize.width; x++) {
			for (int y = 0; y < gridSize.height; y++) {
				this.grid[x][y] = new GridNode(x, y, NodeType.FREE);
			}
		}

	}


	/**
	 * Initialize all the robots in the environment
	 */
	public void init() {
		this.squaresLeft = this.gridSize.width * this.gridSize.height;
		for (int x = 0; x < this.gridSize.width; x++) {
			for (int y = 0; y < this.gridSize.height; y++) {
				if (this.getGridNode(x, y).getNodeType() == NodeType.OBSTACLE) {
					this.squaresLeft--;
				}
			}
		}
		this.stepCount = 1;
		for (int robotNum = 0; robotNum < this.robots.size(); robotNum++) {
			if (this.RANDOMIZE_ROBOT_LOCATION_ON_INIT) {
				Coordinate location = new Coordinate(-1, -1);
				while (location.x == -1 || this.getGridNode(location.x, location.y).getNodeType() == NodeType.OBSTACLE) {
					location.x = (int) (Math.random() * this.getWidth());
					location.y = (int) (Math.random() * this.getHeight());
				}
				this.robots.get(robotNum).setLocation(location.x, location.y);
				if (this.CLEAR_ADJACENT_CELLS_ON_INIT) {
					clearAdjactentCells(this.robots.get(robotNum).getLocation().x, this.robots.get(robotNum).getLocation().y);
				}
				
				this.getGridNode(this.robots.get(robotNum).getLocation().x, this.robots.get(robotNum).getLocation().y)
						.setNodeType(NodeType.FREE);
			}
			this.robots.get(robotNum).coverAlgo.init();

		}
	}


	/**
	 * Clears the four cells directly adjacent to the given grid coordinates
	 * 
	 * @param x
	 *                the x coordinate
	 * @param y
	 *                the y coordinate
	 */
	private void clearAdjactentCells(int x, int y) {
		if (this.isOnGrid(x + 1, y)) {
			this.getGridNode(x + 1, y).setNodeType(NodeType.FREE);
		}
		if (this.isOnGrid(x - 1, y)) {
			this.getGridNode(x - 1, y).setNodeType(NodeType.FREE);
		}
		if (this.isOnGrid(x, y + 1)) {
			this.getGridNode(x, y + 1).setNodeType(NodeType.FREE);
		}
		if (this.isOnGrid(x, y - 1)) {
			this.getGridNode(x, y - 1).setNodeType(NodeType.FREE);
		}
	}


	/**
	 * Moves one time step forward. Robots move, etc.
	 */
	public void step() {
		this.stepCount++;
		for (int robotNum = 0; robotNum < this.robots.size(); robotNum++) {
			if (!this.robots.get(robotNum).isBroken()) {
				this.robots.get(robotNum).coverAlgo.step();
			}
		}
	}


	public String exportToString() {
		StringBuilder sb = new StringBuilder();
		for (int x = 0; x < this.getWidth(); x++) {
			for (int y = 0; y < this.getHeight(); y++) {
				sb.append(String.format("%f ", this.grid[x][y].getDangerProb()));
			}
		}
		return sb.toString();
	}


	/**
	 * Add the given robot to the environment
	 */
	public void addRobot(GridRobot robot) {
		this.robots.add(robot);
	}


	/**
	 * Gets the robot in this environment that has the specified id
	 * 
	 * @param id
	 *                the id of the robot to get
	 * @return the robot
	 */
	public Robot getRobotById(int id) {
		for (int i = 0; i < this.robots.size(); i++) {
			if (this.robots.get(i).getId() == id) {
				return this.robots.get(i);
			}
		}
		return null;
	}


	/**
	 * Returns an array of all the robots at the given (x, y) coordinates
	 * 
	 * @param x
	 *                the x coordinate
	 * @param y
	 *                the y coordinate
	 * @return an array of {@code GridRobot}s
	 */
	public List<GridRobot> getRobotsByLocation(int x, int y) {
		List<GridRobot> robolist = new ArrayList<>();
		for (int i = 0; i < this.robots.size(); i++) {
			if (this.robots.get(i).getLocation().x == x && this.robots.get(i).getLocation().y == y) {
				robolist.add(this.robots.get(i));
			}
		}
		return robolist;
	}


	/**
	 * Gets the list of all robots in the environment
	 * 
	 * @return
	 */
	public List<GridRobot> getRobotList() {
		return this.robots;
	}


	/**
	 * Gets the width of the grid
	 * 
	 * @return the width of the grid
	 */
	public int getWidth() {
		return this.gridSize.width;
	}


	/**
	 * Gets the height of the grid
	 * 
	 * @return the height of the grid
	 */
	public int getHeight() {
		return this.gridSize.height;
	}


	/**
	 * Checks if the given coordinates are on the grid
	 * 
	 * @param x
	 *                the x coordinate
	 * @param y
	 *                the y coordinate
	 * @return true if the coordinates are within the size of this environment's grid,
	 *         false otherwise
	 */
	public boolean isOnGrid(int x, int y) {
		return (0 <= x && x < this.getWidth() && 0 <= y && y < this.getHeight());
	}


	/**
	 * Gets the grid node at the given location
	 * 
	 * @param x
	 * @param y
	 * @return a {@code GridNode}, or null if the given coordinates are not on the
	 *         grid
	 */
	public GridNode getGridNode(int x, int y) {
		if (isOnGrid(x, y)) {
			return this.grid[x][y];
		}
		return null;
	}


	/**
	 * Checks if every grid space has been covered at least once
	 * 
	 * @return true if the graph has been covered at least once, false otherwise
	 */
	public boolean isCovered() {
		return this.squaresLeft <= 0;
		// for (int x = 0; x < this.getWidth(); x++) {
		// for (int y = 0; y < this.getHeight(); y++) {
		// if (this.grid[x][y].getCoverCount() < 1
		// && this.grid[x][y].getNodeType() != NodeType.OBSTACLE) {
		// return false;
		// }
		// }
		// }
		// return true;
	}


	public int getStepCount() {
		return this.stepCount;
	}


	/**
	 * Checks whether a terminal state has been reached. This could happen if the
	 * environment is covered or if there are no robots left (i.e. they all failed).
	 * 
	 * @return true if more steps can be taken, false otherwise
	 */
	public boolean isFinished() {
		return (this.allRobotsBroken() || this.isCovered() || this.MAX_STEPS_PER_RUN <= this.stepCount);
	}


	/**
	 * Checks if all the robots in the environment are broken
	 * 
	 * @return true if all robots are borken, false otherwise
	 */
	public boolean allRobotsBroken() {
		for (Robot r : this.robots) {
			if (!r.isBroken()) {
				return false;
			}
		}
		return true;
	}


	public void reloadSettings() {
		for (Robot r : this.robots) {
			r.reloadSettings();
		}
		this.RANDOMIZE_ROBOT_LOCATION_ON_INIT = AdversarialCoverage.settings.getBooleanProperty("autorun.randomize_robot_start");
		this.MAX_STEPS_PER_RUN = AdversarialCoverage.settings.getIntProperty("autorun.max_steps_per_run");
		this.CLEAR_ADJACENT_CELLS_ON_INIT = AdversarialCoverage.settings.getBooleanProperty("env.clear_adjacent_cells_on_init");
	}
}

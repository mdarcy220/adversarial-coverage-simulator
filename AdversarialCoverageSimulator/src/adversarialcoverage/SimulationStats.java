package adversarialcoverage;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SimulationStats {
	private long nTimeSteps = 0;
	private long totalFreeCells = 0;
	private Set<RobotStats> robotStats = new HashSet<>();
	private GridEnvironment env;
	private long[][] lastCellVisitTimes;


	public SimulationStats(GridEnvironment env, List<GridRobot> robots) {
		this.env = env;
		this.lastCellVisitTimes = new long[env.getWidth()][env.getHeight()];
		for (GridRobot r : robots) {
			this.robotStats.add(new RobotStats(r, env));
		}
		for (int x = 0; x < env.getWidth(); x++) {
			for (int y = 0; y < env.getHeight(); y++) {
				if (env.getGridNode(x, y).getNodeType() == NodeType.FREE) {
					this.totalFreeCells++;
				}
			}
		}
	}


	public long getTotalFreeCells() {
		return this.totalFreeCells;
	}


	public double getFractionCovered() {
		long totalCellsCovered = 0;
		for (int x = 0; x < this.env.getWidth(); x++) {
			for (int y = 0; y < this.env.getHeight(); y++) {
				if (0 < this.env.getGridNode(x, y).getCoverCount()) {
					totalCellsCovered++;
				}
			}
		}

		return ((double) totalCellsCovered) / ((double) this.totalFreeCells);
	}


	public double getAvgCoversPerFreeCell() {
		long totalCovers = 0;
		for (int x = 0; x < this.env.getWidth(); x++) {
			for (int y = 0; y < this.env.getHeight(); y++) {
				if (this.env.getGridNode(x, y).getNodeType() == NodeType.FREE) {
					totalCovers += this.env.getGridNode(x, y).getCoverCount();
				}
			}
		}

		return ((double) totalCovers) / ((double) this.totalFreeCells);
	}


	/**
	 * Gets the cover count of the most-covered cell
	 * 
	 * @return
	 */
	public long getMaxCellCovers() {
		long maxCovers = 0;
		for (int x = 0; x < this.env.getWidth(); x++) {
			for (int y = 0; y < this.env.getHeight(); y++) {
				if (maxCovers < this.env.getGridNode(x, y).getCoverCount()) {
					maxCovers = this.env.getGridNode(x, y).getCoverCount();
				}
			}
		}
		return maxCovers;
	}


	/**
	 * Gets the cover count of the least-covered cell
	 * 
	 * @return
	 */
	public long getMinCellCovers() {
		long minCovers = Long.MAX_VALUE;
		for (int x = 0; x < this.env.getWidth(); x++) {
			for (int y = 0; y < this.env.getHeight(); y++) {
				if (this.env.getGridNode(x, y).getNodeType() == NodeType.FREE
						&& this.env.getGridNode(x, y).getCoverCount() < minCovers) {
					minCovers = this.env.getGridNode(x, y).getCoverCount();
				}
			}
		}
		return minCovers;
	}


	public long getNumBrokenRobots() {
		long nBroken = 0;
		for (Robot r : this.env.getRobotList()) {
			if (r.isBroken()) {
				nBroken++;
			}
		}
		return nBroken;
	}


	public long getNumSurvivingRobots() {
		return this.env.getRobotList().size() - getNumBrokenRobots();
	}


	public long getNumTimeSteps() {
		return this.nTimeSteps;
	}


	public long getTotalCells() {
		return this.env.getHeight() * this.env.getWidth();
	}


	public double getMaxSurvivability() {
		double best = 0.0;
		for (RobotStats rs : this.robotStats) {
			if (best < rs.survivability) {
				best = rs.survivability;
			}
		}
		return best;
	}


	/**
	 * Get the best probability that a robot was able to cover the entire
	 * region
	 * 
	 * @return
	 */
	public double getMaxCoverageProb() {
		double best = 0.0;
		for (RobotStats rs : this.robotStats) {
			if (best < rs.coverageProb) {
				best = rs.coverageProb;
			}
		}
		return best;
	}


	public void updateTimeStep() {
		this.nTimeSteps++;
	}


	public void updateCellCovered(GridRobot r) {
		for (RobotStats rs : this.robotStats) {
			if (rs.robot.equals(r)) {
				rs.updateCellCovered();
			}
		}

		this.lastCellVisitTimes[r.getLocation().x][r.getLocation().y] = this.nTimeSteps;
	}


	public long numFreeCellsCoveredNTimes(long n) {
		long num = 0;
		for (int x = 0; x < this.env.getWidth(); x++) {
			for (int y = 0; y < this.env.getHeight(); y++) {
				if (this.env.getGridNode(x, y).getNodeType() == NodeType.FREE
						&& this.env.getGridNode(x, y).getCoverCount() == n) {
					num++;
				}
			}
		}
		return num;
	}
	
	public RobotStats getRobotStats(GridRobot r) {
		for (RobotStats rs : this.robotStats) {
			if (rs.robot.equals(r)) {
				return rs;
			}
		}
		return null;
	}
}


class RobotStats {
	long pathLength = 0;
	double survivability = 1.0;
	double coverageProb = 1.0;
	GridRobot robot;
	GridEnvironment env;


	public RobotStats(GridRobot robot, GridEnvironment env) {
		this.robot = robot;
		this.env = env;
	}


	public void updateCellCovered() {
		int x = this.robot.getLocation().x;
		int y = this.robot.getLocation().y;

		this.coverageProb *= (1.0 - this.env.getGridNode(x, y).getDangerProb());
		this.survivability += this.coverageProb;
		this.pathLength++;
	}
}
package adversarialcoverage.stats;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import adversarialcoverage.*;

public class SimulationStats {
	private long nStepsInRun = 0;
	private SampledVariableLong batch_stepsPerRun = new SampledVariableLong();
	private long nRunsInBatch = 0;
	private long totalFreeCells = 0;
	private Set<RobotStats> robotStats = new HashSet<>();
	private GridEnvironment env;
	private long[][] lastCellVisitTimes;

	private SampledVariableDouble batch_survivability = new SampledVariableDouble();


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


	public long getTotalCellsCovered() {
		return this.totalFreeCells - this.env.getSquaresLeft();
	}


	public double getFractionCovered() {
		return ((double) this.getTotalCellsCovered()) / ((double) this.totalFreeCells);
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


	public long getNumRobots() {
		return this.env.getRobotList().size();
	}


	public long getNumTimeSteps() {
		return this.nStepsInRun;
	}


	public long getTotalCells() {
		return this.env.getHeight() * this.env.getWidth();
	}


	/**
	 * Returns the maximum individual survivability from the robots.
	 * 
	 * @return
	 */
	public double getMaxSurvivability() {
		double best = 0.0;
		for (RobotStats rs : this.robotStats) {
			if (best < rs.survivability) {
				best = rs.survivability;
			}
		}
		return best;
	}


	public double getTeamSurvivability() {
		double total = 0.0;
		for (RobotStats rs : this.robotStats) {
			total += rs.survivability;

		}
		return total;
	}


	/**
	 * Get the best probability that a robot was able to cover the entire region
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
		this.nStepsInRun++;
	}


	public void updateCellCovered(GridRobot r) {
		for (RobotStats rs : this.robotStats) {
			if (rs.robot.equals(r)) {
				rs.updateCellCovered();
			}
		}

		this.lastCellVisitTimes[r.getLocation().x][r.getLocation().y] = this.nStepsInRun;
	}


	public long numFreeCellsCoveredNTimes(long n) {
		long num = 0;
		for (int x = 0; x < this.env.getWidth(); x++) {
			for (int y = 0; y < this.env.getHeight(); y++) {
				if (this.env.getGridNode(x, y).getNodeType() == NodeType.FREE && this.env.getGridNode(x, y).getCoverCount() == n) {
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


	/**
	 * Get a <code>SampledVariableLong</code> containing sample statistics for the
	 * number of steps taken per run in the current batch.
	 * 
	 * @return
	 */
	public SampledVariableLong getBatchStepsPerRunInfo() {
		return this.batch_stepsPerRun;
	}


	/**
	 * Gets a <code>SampledVariableDouble</code> that can be used to retrieve
	 * information about the survivability per run for the batch.
	 * 
	 * @return
	 */
	public SampledVariableDouble getBatchSurvivability() {
		return this.batch_survivability;
	}


	/**
	 * Gets the number of runs that are in the current batch
	 * 
	 * @return
	 */
	public long getRunsInCurrentBatch() {
		return this.nRunsInBatch;
	}


	/**
	 * Resets the statistics for an individual coverage. Batch statistics will remain
	 * intact.
	 */
	public void startNewRun() {
		this.batch_stepsPerRun.addSample(this.nStepsInRun);
		this.batch_survivability.addSample(this.getTeamSurvivability());
		this.nRunsInBatch++;

		resetRunStats();
	}


	private void resetRunStats() {
		this.nStepsInRun = 0;
		this.totalFreeCells = 0;
		this.lastCellVisitTimes = new long[this.env.getWidth()][this.env.getHeight()];
		for (int x = 0; x < this.env.getWidth(); x++) {
			for (int y = 0; y < this.env.getHeight(); y++) {
				if (this.env.getGridNode(x, y).getNodeType() == NodeType.FREE) {
					this.totalFreeCells++;
				}
			}
		}
		for (RobotStats rs : this.robotStats) {
			rs.reset();
		}
	}


	/**
	 * Reset batch stats
	 */
	public void startNewBatch() {
		this.batch_stepsPerRun.reset();
		this.nRunsInBatch = 0;
		this.batch_survivability.reset();
	}


	/**
	 * Reset all statistics.
	 */
	public void reset() {
		resetRunStats();
	}


}


class RobotStats {
	long pathLength = 0;
	double survivability = 0.0;
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
		if (this.env.getGridNode(x, y).getCoverCount() <= 1) {
			this.survivability += this.coverageProb;
		}
		this.pathLength++;
	}


	public void reset() {
		this.pathLength = 0;
		this.survivability = 0.0;
		this.coverageProb = 1.0;
	}
}

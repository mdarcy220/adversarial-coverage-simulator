package simulations.coverage;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import adsim.*;
import adsim.stats.SampledVariableDouble;
import adsim.stats.SampledVariableLong;
import gridenv.GridEnvironment;
import gridenv.GridNode;
import gridenv.GridRobot;


public class CoverageStats {
	private long nStepsInRun = 0;
	private SampledVariableLong batch_stepsPerRun = new SampledVariableLong();
	private long nRunsInBatch = 0;
	private long totalFreeCells = 0;
	private long squaresLeft;
	private Set<RobotStats> robotStats = new HashSet<>();
	private GridEnvironment env;
	private long[][] lastCellVisitTimes;

	private SampledVariableDouble batch_survivability = new SampledVariableDouble();
	private SampledVariableDouble batch_coverage = new SampledVariableDouble();


	public CoverageStats(GridEnvironment env, List<GridRobot> robots) {
		this.env = env;
		this.lastCellVisitTimes = new long[env.getWidth()][env.getHeight()];
		for (GridRobot r : robots) {
			this.robotStats.add(new RobotStats(r, env));
		}
		this.resetRunStats();
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
	 * Gets a <code>SampledVariableDouble</code> that can be used to retrieve
	 * information about the fraction of the grid covered per run for the batch.
	 * 
	 * @return
	 */
	public SampledVariableDouble getBatchCoverage() {
		return this.batch_coverage;
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


	public double getFractionCovered() {
		return ((double) this.getTotalCellsCovered()) / ((double) this.totalFreeCells);
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


	public long getNumRobots() {
		return this.env.getRobotList().size();
	}


	public long getNumSurvivingRobots() {
		return this.env.getRobotList().size() - getNumBrokenRobots();
	}


	public long getNumTimeSteps() {
		return this.nStepsInRun;
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
	 * Gets the number of runs that are in the current batch
	 * 
	 * @return
	 */
	public long getRunsInCurrentBatch() {
		return this.nRunsInBatch;
	}


	public double getTeamSurvivability() {
		double total = 0.0;
		for (RobotStats rs : this.robotStats) {
			total += rs.survivability;

		}
		return total;
	}


	public long getTotalCells() {
		return this.env.getHeight() * this.env.getWidth();
	}


	public long getTotalCellsCovered() {
		return this.totalFreeCells - this.squaresLeft;
	}


	public long getTotalFreeCells() {
		return this.totalFreeCells;
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


	/**
	 * Reset all statistics.
	 */
	public void reset() {
		resetRunStats();
		resetBatchStats();
	}


	/**
	 * Reset batch stats
	 */
	public void resetBatchStats() {
		this.nRunsInBatch = 0;
		this.batch_stepsPerRun.reset();
		this.batch_survivability.reset();
		this.batch_coverage.reset();
	}


	public void resetRunStats() {
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
		this.squaresLeft = this.totalFreeCells;

		for (RobotStats rs : this.robotStats) {
			rs.reset();
		}
	}


	/**
	 * Resets the statistics for an individual coverage. Batch statistics will remain
	 * intact.
	 */
	public void startNewRun() {
		this.batch_stepsPerRun.addSample(this.nStepsInRun);
		this.batch_survivability.addSample(this.getTeamSurvivability());
		this.batch_coverage.addSample(this.getFractionCovered() * 100.0);
		this.nRunsInBatch++;

		resetRunStats();
	}


	public void updateCellCovered(GridRobot r) {
		for (RobotStats rs : this.robotStats) {
			if (rs.robot.equals(r)) {
				rs.updateCellCovered();
			}
		}

		this.lastCellVisitTimes[r.getLocation().x][r.getLocation().y] = this.nStepsInRun;

		if (this.env.getGridNode(r.getLocation().x, r.getLocation().y).getCoverCount() == 0) {
			this.squaresLeft--;
		}
	}


	public void updateTimeStep() {
		this.nStepsInRun++;
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


	public void reset() {
		this.pathLength = 0;
		this.survivability = 0.0;
		this.coverageProb = 1.0;
	}


	public void updateCellCovered() {
		int x = this.robot.getLocation().x;
		int y = this.robot.getLocation().y;
		GridNode node = this.env.getGridNode(x, y);

		if (node.getCoverCount() < 1) {
			this.survivability += this.coverageProb;
		}
		this.coverageProb *= (1.0 - node.getDangerProb());

		this.pathLength++;
	}
}

package simulations.coverage;

import adsim.NodeType;
import adsim.Simulation;
import adsim.SimulatorMain;
import adsim.stats.SampledVariableDouble;
import adsim.stats.SampledVariableLong;
import gridenv.GridEnvironment;

public class CoverageSimulation implements Simulation {

	GridEnvironment env = null;
	public int squaresLeft = 0;
	private int MAX_STEPS_PER_RUN = SimulatorMain.settings.getInt("autorun.max_steps_per_run");
	private double COVER_UNIQUE_REWARD = SimulatorMain.settings.getDouble("deepql.reward.cover_unique");
	private double COVER_AGAIN_REWARD = SimulatorMain.settings.getDouble("deepql.reward.cover_again");
	private double DEATH_REWARD = SimulatorMain.settings.getDouble("deepql.reward.death");
	private double FULL_COVERAGE_REWARD = SimulatorMain.settings.getDouble("deepql.reward.full_coverage");


	public CoverageSimulation() {

	}


	@Override
	public void init() {

	}


	@Override
	public void onEnvInit() {
		if (this.env == null) {
			return;
		}
		this.squaresLeft = this.env.gridSize.width * this.env.gridSize.height;
		for (

				int x = 0; x < this.env.gridSize.width; x++) {
			for (int y = 0; y < this.env.gridSize.height; y++) {
				if (this.env.getGridNode(x, y).getNodeType() == NodeType.OBSTACLE || 0 < this.env.getGridNode(x, y).getCoverCount()) {
					this.squaresLeft--;
				}
			}
		}
	}


	/**
	 * Checks if every grid space has been covered at least once
	 * 
	 * @return true if the graph has been covered at least once, false otherwise
	 */
	private boolean isCovered() {
		return this.squaresLeft <= 0;
	}


	@Override
	public void setEnvironment(GridEnvironment env) {
		this.env = env;
	}


	@Override
	public void onRunEnd() {
		long statsBatchSize = SimulatorMain.settings.getInt("stats.multirun.batch_size");
		CoverageStats stats = SimulatorMain.getStats();
		if (this.isTerminalState() && stats != null) {
			System.out.printf("Run end: steps=%d, cov=%d/%d, tSv=%.3f, bots=%d/%d\n", stats.getNumTimeSteps(),
					stats.getTotalCellsCovered(), stats.getTotalFreeCells(), stats.getTeamSurvivability(),
					stats.getNumSurvivingRobots(), stats.getNumRobots());
			if (statsBatchSize <= stats.getRunsInCurrentBatch()) {
				final SampledVariableLong stepsPerRunInfo = stats.getBatchStepsPerRunInfo();
				final SampledVariableDouble survivabilityInfo = stats.getBatchSurvivability();
				final SampledVariableDouble coverageInfo = stats.getBatchCoverage();
				System.out.printf("Batch end (size=%d): steps=%.1f (%.1f), cov=%.1f%% (%.1f), tSv=%.2f (%.1f)\n",
						stats.getRunsInCurrentBatch(), stepsPerRunInfo.mean(), stepsPerRunInfo.stddev(), coverageInfo.mean(),
						coverageInfo.stddev(), survivabilityInfo.mean(), survivabilityInfo.stddev());
				stats.resetBatchStats();
			}
		}
	}


	@Override
	public boolean isTerminalState() {
		// Terminal states occur when the environment is covered, all robots are
		// dead, or the maximum allowed number of steps has been reached
		return (this.env.allRobotsBroken() || this.isCovered() || this.MAX_STEPS_PER_RUN <= this.env.getStepCount());
	}


	@Override
	public void onStep() {

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
	public double getCellCoverageReward(int oldCoverCount, boolean isThreat) {
		double reward = 0.0;
		if (oldCoverCount == 0) {
			this.squaresLeft--;
		}

		if (isThreat) {
			reward = this.DEATH_REWARD;

		} else {
			reward = oldCoverCount < 1 ? this.COVER_UNIQUE_REWARD : this.COVER_AGAIN_REWARD;

			if (this.isCovered()) {
				reward = this.FULL_COVERAGE_REWARD;
			}
		}
		return reward;
	}


	@Override
	public void reloadSettings() {
		this.COVER_UNIQUE_REWARD = SimulatorMain.settings.getDouble("deepql.reward.cover_unique");
		this.COVER_AGAIN_REWARD = SimulatorMain.settings.getDouble("deepql.reward.cover_again");
		this.DEATH_REWARD = SimulatorMain.settings.getDouble("deepql.reward.death");
		this.FULL_COVERAGE_REWARD = SimulatorMain.settings.getDouble("deepql.reward.full_coverage");
		this.MAX_STEPS_PER_RUN = SimulatorMain.settings.getInt("autorun.max_steps_per_run");
	}

}

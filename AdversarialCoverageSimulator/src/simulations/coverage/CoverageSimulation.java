package simulations.coverage;

import java.awt.Dimension;
import java.awt.GraphicsEnvironment;

import adsim.Algorithm;
import adsim.ConsoleController;
import adsim.DisplayAdapter;
import adsim.NodeType;
import adsim.Simulation;
import adsim.SimulatorEngine;
import adsim.SimulatorMain;
import adsim.TerminalCommand;
import adsim.stats.SampledVariableDouble;
import adsim.stats.SampledVariableLong;
import gridenv.GridEnvironment;
import gridenv.GridRobot;
import gridenv.GridSensor;
import simulations.coverage.algo.GSACGC;
import simulations.generic.algo.DQL;
import simulations.generic.algo.ExternalDQL;
import simulations.generic.algo.RandomActionAlgo;

public class CoverageSimulation implements Simulation {

	GridEnvironment env = null;
	SimulatorEngine engine = null;
	public int squaresLeft = 0;
	private int MAX_STEPS_PER_RUN = SimulatorMain.settings.getInt("autorun.max_steps_per_run");


	public CoverageSimulation() {

	}


	private Algorithm createNewCoverageAlgoInstance(GridRobot robot) {
		GridSensor sensor = new GridSensor(this.env, robot);
		CoverageGridActuator actuator = new CoverageGridActuator(this.env, robot, this);

		String coverageAlgoName = SimulatorMain.settings.getString("adsim.algorithm_name");
		String metaCoverageAlgoName = "";

		Algorithm algo = null;

		if (coverageAlgoName.indexOf('+') != -1) {
			metaCoverageAlgoName = coverageAlgoName.substring(0, coverageAlgoName.indexOf('+')).trim();
			coverageAlgoName = coverageAlgoName.substring(coverageAlgoName.indexOf('+') + 1).trim();
		}

		if (coverageAlgoName.equalsIgnoreCase("DQL")) {
			algo = new DQL(sensor, actuator);
		} else if (coverageAlgoName.equalsIgnoreCase("RandomGC")) {
			algo = new RandomActionAlgo(sensor, actuator);
		} else if (coverageAlgoName.equalsIgnoreCase("GSACGC")) {
			algo = new GSACGC(sensor, actuator);
		} else {
			algo = new DQL(sensor, actuator);
		}

		if (!metaCoverageAlgoName.isEmpty()) {
			if (metaCoverageAlgoName.equalsIgnoreCase("ExternalDQL")) {
				algo = new ExternalDQL(sensor, actuator, algo);
			}
		}

		return algo;
	}


	@Override
	public void init() {
		this.registerConsoleCommands();
		if (!SimulatorMain.args.HEADLESS) {
			GUIDisplay gd = GUIDisplay.createInstance(this);
			if (gd != null) {
				gd.setup();
				this.engine.setDisplay(gd);
			}
		}
	}


	private void registerConsoleCommands() {
		final ConsoleController controller = SimulatorMain.controller;
		controller.registerCommand(":setdisplay", new TerminalCommand() {
			@Override
			public void execute(String[] args) {
				if (args.length < 1) {
					return;
				}
				DisplayAdapter display = null;
				if (args[0].equals("gui")) {
					if (GraphicsEnvironment.isHeadless()) {
						return;
					}
					GUIDisplay gd = GUIDisplay.createInstance(CoverageSimulation.this);
					gd.setup();
					display = gd;
				} else if (args[0].equals("none")) {
					display = null;
				}
				CoverageSimulation.this.engine.setDisplay(display);
			}
		});


		controller.registerCommand(":restart", new TerminalCommand() {
			@Override
			public void execute(String[] args) {
				CoverageSimulation.this.restartSimulation();
			}
		});
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
	public boolean isTerminalState() {
		// Terminal states occur when the environment is covered, all robots are
		// dead, or the maximum allowed number of steps has been reached
		return (this.env.allRobotsBroken() || this.isCovered() || this.MAX_STEPS_PER_RUN <= this.env.getStepCount());
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
	 * Event hook for cell coverage
	 */
	public void onNewCellCovered() {
		this.squaresLeft--;
	}


	@Override
	public void onNewRun() {
		this.resetEnvironment();
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

		if (SimulatorMain.settings.getBoolean("autorun.finished.newgrid")) {
			for (GridRobot r : this.env.getRobotList()) {
				r.setBroken(false);
			}

			this.env.regenerateGrid();
			this.env.init();

		} else {
			this.engine.pauseSimulation();
		}
	}


	@Override
	public void onStep() {
		this.env.step();
	}


	public void restartSimulation() {
		this.engine.pauseSimulation();
		reinitializeSimulation();
		this.engine.refreshDisplay();
	}


	private void reinitializeSimulation() {
		for (int x = 0; x < this.env.getWidth(); x++) {
			for (int y = 0; y < this.env.getHeight(); y++) {
				this.env.getGridNode(x, y).setCoverCount(0);
			}
		}
		this.env.init();
	}


	@Override
	public void reloadSettings() {
		this.env.reloadSettings();
		this.MAX_STEPS_PER_RUN = SimulatorMain.settings.getInt("autorun.max_steps_per_run");
	}


	/**
	 * Sets up the environment using the settings
	 */
	private void resetEnvironment() {
		this.env = new GridEnvironment(
				new Dimension(SimulatorMain.settings.getInt("env.grid.width"), SimulatorMain.settings.getInt("env.grid.height")));

		// Set up the coverage environment
		this.env.regenerateGrid();

		// Set up the robots
		for (int i = 0; i < SimulatorMain.settings.getInt("robots.count"); i++) {
			GridRobot robot = new GridRobot(i, (int) (Math.random() * this.env.getWidth()), (int) (Math.random() * this.env.getHeight()));
			robot.coverAlgo = this.createNewCoverageAlgoInstance(robot);
			this.env.addRobot(robot);
		}
		SimulatorMain.setStats(new CoverageStats(this.env, this.env.getRobotList()));
		SimulatorMain.getStats().resetBatchStats();

		this.env.init();
	}


	@Override
	public void setEngine(SimulatorEngine engine) {
		this.engine = engine;
	}


	public SimulatorEngine getEngine() {
		return this.engine;
	}


	public GridEnvironment getEnv() {
		return this.env;
	}

}

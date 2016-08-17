package simulations.coverage;

import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.util.Random;

import adsim.Algorithm;
import adsim.ConsoleController;
import adsim.Display;
import adsim.NodeType;
import adsim.Simulation;
import adsim.SimulatorEngine;
import adsim.SimulatorMain;
import adsim.TerminalCommand;
import adsim.stats.SampledVariableDouble;
import adsim.stats.SampledVariableLong;
import gridenv.GridEnvironment;
import gridenv.GridNodeGenerator;
import gridenv.GridRobot;
import gridenv.GridSensor;
import simulations.coverage.algo.GSACGC;
import simulations.coverage.display.CoverageGUIDisplay;
import simulations.generic.algo.DQL;
import simulations.generic.algo.ExternalDQL;
import simulations.generic.algo.RandomActionAlgo;

public class CoverageSimulation implements Simulation {

	GridEnvironment env = null;
	SimulatorEngine engine = null;
	public int squaresLeft = 0;
	private Random random = new Random();
	private GridNodeGenerator nodegen = new GridNodeGenerator();
	private int MAX_STEPS_PER_RUN = SimulatorMain.settings.getInt("autorun.max_steps_per_run");
	private boolean VARIABLE_GRID_SIZE = SimulatorMain.settings.getBoolean("env.variable_grid_size");
	private boolean FORCE_SQUARE = SimulatorMain.settings.getBoolean("env.grid.force_square");
	private int MAX_HEIGHT = SimulatorMain.settings.getInt("env.grid.maxheight");
	private int MAX_WIDTH = SimulatorMain.settings.getInt("env.grid.maxwidth");
	private int MIN_HEIGHT = SimulatorMain.settings.getInt("env.grid.minheight");
	private int MIN_WIDTH = SimulatorMain.settings.getInt("env.grid.minwidth");


	public CoverageSimulation() {

	}


	private Algorithm createNewCoverageAlgoInstance(GridRobot robot) {
		GridSensor sensor = new GridSensor(this.env, robot);
		CoverageActuator actuator = new CoverageActuator(this.env, robot, this);

		String coverageAlgoName = SimulatorMain.settings.getString("adsim.algorithm_name");
		String metaCoverageAlgoName = "";

		Algorithm algo = null;

		if (coverageAlgoName.indexOf('+') != -1) {
			metaCoverageAlgoName = coverageAlgoName.substring(0, coverageAlgoName.indexOf('+')).trim();
			coverageAlgoName = coverageAlgoName.substring(coverageAlgoName.indexOf('+') + 1).trim();
		}

		if (coverageAlgoName.equalsIgnoreCase("DQL")) {
			algo = new DQL(sensor, actuator);
			((DQL) algo).setStatePreprocessor(new CoverageStatePreprocessor(sensor));
		} else if (coverageAlgoName.equalsIgnoreCase("Random")) {
			algo = new RandomActionAlgo(sensor, actuator);
		} else if (coverageAlgoName.equalsIgnoreCase("GSACGC")) {
			algo = new GSACGC(sensor, actuator);
		} else {
			algo = new DQL(sensor, actuator);
			((DQL) algo).setStatePreprocessor(new CoverageStatePreprocessor(sensor));
		}

		if (!metaCoverageAlgoName.isEmpty()) {
			if (metaCoverageAlgoName.equalsIgnoreCase("ExternalDQL")) {
				algo = new ExternalDQL(sensor, actuator, algo);
				((ExternalDQL) algo).setStatePreprocessor(new CoverageStatePreprocessor(sensor));
			}
		}


		return algo;
	}


	@Override
	public void init() {
		this.registerConsoleCommands();
		if (!SimulatorMain.args.HEADLESS) {
			CoverageGUIDisplay gd = CoverageGUIDisplay.createInstance(this);
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
				Display display = null;
				if (args[0].equals("gui")) {
					if (GraphicsEnvironment.isHeadless()) {
						return;
					}
					CoverageGUIDisplay gd = CoverageGUIDisplay.createInstance(CoverageSimulation.this);
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

			this.regenerateGrid();
			this.env.init();

		} else {
			this.engine.pauseSimulation();
		}
	}


	private void regenerateGrid() {
		if (this.VARIABLE_GRID_SIZE) {
			int newWidth = (int) (this.random.nextDouble() * (this.MAX_WIDTH - this.MIN_WIDTH) + this.MIN_WIDTH);
			int newHeight = (int) (this.random.nextDouble() * (this.MAX_HEIGHT - this.MIN_HEIGHT) + this.MIN_HEIGHT);
			if (this.FORCE_SQUARE) {
				newHeight = newWidth;
			}

			this.env.setSize(new Dimension(newWidth, newHeight));
		}

		String dangerValStr = SimulatorMain.settings.getString("env.grid.dangervalues");

		// To save time, only recompile the generator if the string has changed
		if (!this.nodegen.getGeneratorString().equals(dangerValStr)) {
			this.nodegen.setGeneratorString(dangerValStr);
		}

		this.nodegen.setRandomMap();

		int gridWidth = this.env.getWidth();
		int gridHeight = this.env.getHeight();

		for (int x = 0; x < gridWidth; x++) {
			for (int y = 0; y < gridHeight; y++) {
				this.nodegen.genNext(this.env.grid[x][y]);
			}
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
		this.VARIABLE_GRID_SIZE = SimulatorMain.settings.getBoolean("env.variable_grid_size");
		this.FORCE_SQUARE = SimulatorMain.settings.getBoolean("env.grid.force_square");
		this.MAX_HEIGHT = SimulatorMain.settings.getInt("env.grid.maxheight");
		this.MAX_WIDTH = SimulatorMain.settings.getInt("env.grid.maxwidth");
		this.MIN_HEIGHT = SimulatorMain.settings.getInt("env.grid.minheight");
		this.MIN_WIDTH = SimulatorMain.settings.getInt("env.grid.minwidth");
	}


	/**
	 * Sets up the environment using the settings
	 */
	private void resetEnvironment() {
		this.env = new GridEnvironment(
				new Dimension(SimulatorMain.settings.getInt("env.grid.width"), SimulatorMain.settings.getInt("env.grid.height")));

		// Set up the coverage environment
		this.regenerateGrid();

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


	@Override
	public void dispose() {
		// TODO Auto-generated method stub
		
	}

}

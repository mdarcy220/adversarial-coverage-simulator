package adsim;

import java.awt.Dimension;
import java.io.PrintStream;

import adsim.stats.*;
import algo.*;
import algo.coverage.GSACGC;
import algo.coverage.GridCoverageAlgorithm;
import algo.coverage.RandomGC;
import gridenv.GridActuator;
import gridenv.GridEnvironment;
import gridenv.GridRobot;
import gridenv.GridSensor;

public class SimulatorEngine {

	private boolean isRunning = false;
	private DisplayAdapter display = null;
	private GridEnvironment env = null;
	private Thread simulationThread = null;
	private static final DisplayAdapter EmptyDisplayAdapter = new DisplayAdapter() {
		@Override
		public void refresh() {
			// Do nothing
		}


		@Override
		public void dispose() {
			// Do nothing
		}
	};


	public SimulatorEngine(DisplayAdapter display) {
		this.setDisplay(display);
		this.init();
	}


	public SimulatorEngine() {
		this(SimulatorEngine.EmptyDisplayAdapter);
	}


	public void init() {

	}


	public GridEnvironment getEnv() {
		return this.env;
	}


	public void stepSimulation() {
		if (!this.env.isFinished()) {
			this.step();
		}
		refreshDisplay();
	}


	public void runSimulation() {
		this.isRunning = true;
		startSimulationLoop();
	}


	public void pauseSimulation() {
		this.isRunning = false;
	}


	public void restartSimulation() {
		this.isRunning = false;
		reinitializeSimulation();
		refreshDisplay();
	}


	public void newSimulation() {
		this.isRunning = false;
		this.resetEnvironment();
		refreshDisplay();
	}


	/**
	 * Updates the display, which may be a GUI window, or in a headless environment, a
	 * terminal.
	 */
	public void refreshDisplay() {
		this.display.refresh();
	}


	public void setDisplay(DisplayAdapter newDisplay) {
		if (this.display != null) {
			this.display.dispose();
		}

		if (newDisplay != null) {
			this.display = newDisplay;
		} else {
			this.display = SimulatorEngine.EmptyDisplayAdapter;
		}
	}


	private void reinitializeSimulation() {
		for (int x = 0; x < this.env.getWidth(); x++) {
			for (int y = 0; y < this.env.getHeight(); y++) {
				this.env.getGridNode(x, y).setCoverCount(0);
			}
		}
		this.env.init();
	}


	/**
	 * Sets up the environment using the settings
	 */
	public void resetEnvironment() {
		this.env = new GridEnvironment(new Dimension(SimulatorMain.settings.getInt("env.grid.width"),
				SimulatorMain.settings.getInt("env.grid.height")));


		// Set up the coverage environment
		this.env.regenerateGrid();

		// Set up the robots
		for (int i = 0; i < SimulatorMain.settings.getInt("robots.count"); i++) {
			GridRobot robot = new GridRobot(i, (int) (Math.random() * this.env.getWidth()), (int) (Math.random() * this.env.getHeight()));
			robot.coverAlgo = this.createNewCoverageAlgoInstance(robot);
			this.env.addRobot(robot);
		}
		SimulatorMain.setStats(new SimulationStats(this.env, this.env.getRobotList()));
		SimulatorMain.getStats().resetBatchStats();

		this.env.init();

	}


	private GridCoverageAlgorithm createNewCoverageAlgoInstance(GridRobot robot) {
		GridSensor sensor = new GridSensor(this.env, robot);
		GridActuator actuator = new GridActuator(this.env, robot);

		String coverageAlgoName = SimulatorMain.settings.getString("adsim.algorithm_name");
		String metaCoverageAlgoName = "";

		GridCoverageAlgorithm algo = null;

		if (coverageAlgoName.indexOf('+') != -1) {
			metaCoverageAlgoName = coverageAlgoName.substring(0, coverageAlgoName.indexOf('+')).trim();
			coverageAlgoName = coverageAlgoName.substring(coverageAlgoName.indexOf('+') + 1).trim();
		}

		if (coverageAlgoName.equalsIgnoreCase("DQL")) {
			algo = new DQL(sensor, actuator);
		} else if (coverageAlgoName.equalsIgnoreCase("RandomGC")) {
			algo = new RandomGC(sensor, actuator);
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


	private void startSimulationLoop() {
		if (this.simulationThread != null && this.simulationThread.isAlive()) {
			System.err.print("Coverage thread is already running. No action will be taken.\n");
			return;
		}

		this.simulationThread = new Thread() {
			@Override
			public void run() {
				simulationLoop();
			}
		};
		this.simulationThread.start();
	}


	private void simulationLoop() {

		// Update settings
		this.env.reloadSettings();

		long delay = SimulatorMain.settings.getInt("autorun.stepdelay");
		boolean doRepaint = SimulatorMain.settings.getBoolean("autorun.do_repaint");

		while (this.isRunning) {
			long time = System.currentTimeMillis();
			this.step();
			if (doRepaint && !SimulatorMain.args.HEADLESS) {
				refreshDisplay();
			}
			if (this.env.isFinished()) {
				handleSimulationCompletion();
			}

			time = System.currentTimeMillis() - time;
			if (time < delay) {
				try {
					Thread.sleep(delay - time);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}


	private void handleSimulationCompletion() {
		long statsBatchSize = SimulatorMain.settings.getInt("stats.multirun.batch_size");
		SimulationStats stats = SimulatorMain.getStats();
		if (this.env.isFinished() && stats != null) {
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

		if (SimulatorMain.settings.getBoolean("autorun.finished.display_full_stats")) {
			SimulatorMain.printStats(new PrintStream(System.out));
		}
		if (SimulatorMain.settings.getBoolean("autorun.finished.newgrid")) {
			for (GridRobot r : this.env.getRobotList()) {
				r.setBroken(false);
			}

			this.env.regenerateGrid();
			this.env.init();

			refreshDisplay();

		} else {
			this.isRunning = false;
		}

		if (stats != null) {
			stats.startNewRun();
		}
	}


	private void step() {
		this.env.step();
		SimulatorMain.getStats().updateTimeStep();
	}


	public boolean isRunning() {
		return this.isRunning;
	}


	public void kill() {
		this.isRunning = false;
		this.display.dispose();
	}

}

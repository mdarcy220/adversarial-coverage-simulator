package adversarialcoverage;

import java.awt.Dimension;
import java.io.PrintStream;

public class CoverageEngine {

	private boolean isRunning = false;
	private DisplayAdapter display = null;
	private GridEnvironment env = null;


	public CoverageEngine(DisplayAdapter display) {
		this.display = display;
		this.init();
	}
	
	
	public CoverageEngine() {
		this(new DisplayAdapter() {
			@Override
			public void refresh() {
				// Do nothing
			}
		});
	}


	public void init() {

	}
	
	
	public GridEnvironment getEnv() {
		return this.env;
	}


	public void stepCoverage() {
		if (!this.env.isFinished()) {
			this.env.step();
		}
		refreshDisplay();
	}


	public void runCoverage() {
		this.isRunning = true;
		startCoverageLoop();
	}


	public void pauseCoverage() {
		this.isRunning = false;
	}


	public void restartCoverage() {
		this.isRunning = false;
		// resetCoverageEnvironment();
		reinitializeCoverage();
		refreshDisplay();
	}


	public void newCoverage() {
		this.isRunning = false;
		this.resetCoverageEnvironment();
		refreshDisplay();
	}


	/**
	 * Updates the display, which may be a GUI window, or in a headless environment, a
	 * terminal.
	 */
	public void refreshDisplay() {
		this.display.refresh();
	}


	public void setDisplay(DisplayAdapter display) {
		this.display = display;
	}


	private void reinitializeCoverage() {
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
	public void resetCoverageEnvironment() {
		this.env = new GridEnvironment(new Dimension(AdversarialCoverage.settings.getIntProperty("env.grid.width"),
				AdversarialCoverage.settings.getIntProperty("env.grid.height")));


		// Set up the coverage environment
		genGridFromDangerValuesString(AdversarialCoverage.settings.getStringProperty("env.grid.dangervalues"));


		// Set up the robots
		for (int i = 0; i < AdversarialCoverage.settings.getIntProperty("robots.count"); i++) {
			GridRobot robot = new GridRobot(i, (int) (Math.random() * this.env.getWidth()), (int) (Math.random() * this.env.getHeight()));
			GridSensor sensor = new GridSensor(this.env, robot);
			GridActuator actuator = new GridActuator(this.env, robot);
			CoverageAlgorithm algo = new DeepQLGridCoverage(sensor, actuator);
			robot.coverAlgo = algo;
			this.env.addRobot(robot);
		}
		AdversarialCoverage.stats = new SimulationStats(this.env, this.env.getRobotList());

		this.env.init();

	}


	private void startCoverageLoop() {
		Thread t = new Thread() {
			@Override
			public void run() {
				coverageLoop();
			}
		};
		t.start();

	}


	private void coverageLoop() {
		// Update settings
		this.env.reloadSettings();

		long delay = AdversarialCoverage.settings.getIntProperty("autorun.stepdelay");
		boolean doRepaint = AdversarialCoverage.settings.getBooleanProperty("autorun.do_repaint");

		while (this.isRunning) {
			long time = System.currentTimeMillis();
			this.step();
			if (doRepaint && !AdversarialCoverage.args.HEADLESS) {
				refreshDisplay();
			}
			if (this.env.isFinished()) {
				handleCoverageCompletion();
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


	private void handleCoverageCompletion() {
		long statsBatchSize = AdversarialCoverage.settings.getIntProperty("stats.multirun.batch_size");
		if (this.env.isCovered()) {
			System.out.printf("Covered the environment in %d steps.\n", AdversarialCoverage.stats.getNumTimeSteps());

			if (AdversarialCoverage.stats.getRunsInCurrentBatch() % statsBatchSize == 0) {
				System.out.printf("Average steps for last %d coverages: %f\n", AdversarialCoverage.stats.getRunsInCurrentBatch(),
						AdversarialCoverage.stats.getBatchAvgSteps());
				AdversarialCoverage.stats.reset();
			}
		}

		if (AdversarialCoverage.settings.getBooleanProperty("autorun.finished.display_full_stats")) {
			AdversarialCoverage.printStats(new PrintStream(System.out));
		}
		if (AdversarialCoverage.settings.getBooleanProperty("autorun.finished.newgrid")) {
			for (GridRobot r : this.env.getRobotList()) {
				r.setBroken(false);
			}

			genGridFromDangerValuesString(AdversarialCoverage.settings.getStringProperty("env.grid.dangervalues"));
			this.env.init();

			refreshDisplay();

		} else {
			this.isRunning = false;
		}
		AdversarialCoverage.stats.startNewRun();
	}


	/**
	 * Creates a grid using the given danger values string.
	 * 
	 * @param dangerValStr
	 *                the string to be used when generating the grid.
	 */
	private void genGridFromDangerValuesString(String dangerValStr) {
		GridNodeGenerator nodegen = new GridNodeGenerator();
		nodegen.useScanner(dangerValStr);
		for (int x = 0; x < this.env.getWidth(); x++) {
			for (int y = 0; y < this.env.getHeight(); y++) {
				nodegen.genNext(this.env.getGridNode(x, y));
			}
		}
		nodegen = null;
	}


	private void step() {
		this.env.step();
		AdversarialCoverage.stats.updateTimeStep();
	}


	public String isRunning() {
		return (new Boolean(this.isRunning)).toString();
	}

}

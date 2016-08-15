package adsim;

import gridenv.GridEnvironment;
import gui.GUIDisplay;
import simulations.coverage.CoverageSimulation;
import simulations.coverage.CoverageStats;

public class SimulatorMain {

	public static SimulatorArgs args = null;
	public static SimulatorSettings settings = null;
	public static Logger logger = null;
	public static ConsoleController controller = null;

	GridEnvironment env = null;
	private static CoverageStats stats;
	private static SimulatorEngine engine = null;


	public SimulatorMain(String argsArr[]) {
		// Set up args and settings first
		args = new SimulatorArgs(argsArr);
		settings = new SimulatorSettings();

		// Set up the logger
		logger = new Logger();

		SimulatorMain.engine = new SimulatorEngine(new CoverageSimulation());
		SimulatorMain.controller = new ConsoleController(SimulatorMain.engine);

		SimulatorMain.engine.resetEnvironment();

		if (!args.RC_FILE.equals("")) {
			SimulatorMain.controller.loadCommandFile(args.RC_FILE);
		}
		SimulatorMain.controller.start();


		if (!args.HEADLESS) {
			GUIDisplay gd = GUIDisplay.createInstance(SimulatorMain.engine);
			if (gd != null) {
				gd.setup();
				SimulatorMain.engine.setDisplay(gd);
			}
		}

		if (args.USE_AUTOSTART && !SimulatorMain.engine.isRunning()) {
			SimulatorMain.engine.runSimulation();
		}

	}


	public static SimulatorEngine getEngine() {
		return engine;
	}


	public static void main(String[] argsArr) {
		new SimulatorMain(argsArr);
	}


	public static CoverageStats getStats() {
		return stats;
	}


	public static void setStats(CoverageStats stats) {
		SimulatorMain.stats = stats;
	}
}

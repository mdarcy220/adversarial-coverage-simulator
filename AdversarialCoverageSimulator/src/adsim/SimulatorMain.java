package adsim;

import gridenv.GridEnvironment;
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
		controller = new ConsoleController();
		settings = new SimulatorSettings();

		// Set up the logger
		logger = new Logger();
		
		controller.setSimulatorSettings(settings);

		SimulatorMain.engine = new SimulatorEngine(new CoverageSimulation());
		SimulatorMain.engine.newRun();

		if (!args.RC_FILE.equals("")) {
			SimulatorMain.controller.loadCommandFile(args.RC_FILE);
		}
		SimulatorMain.controller.start();


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

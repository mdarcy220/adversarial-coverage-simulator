package adsim;

import java.io.PrintStream;

import adsim.stats.SimulationStats;
import gridenv.GridEnvironment;
import gui.GUIDisplay;

public class SimulatorMain {

	public static SimulatorArgs args = null;
	public static SimulatorSettings settings = null;
	public static Logger logger = null;
	public static ConsoleController controller = null;

	GridEnvironment env = null;
	private static SimulationStats stats;
	private SimulatorEngine engine = null;


	public SimulatorMain(String argsArr[]) {
		// Set up args and settings first
		args = new SimulatorArgs(argsArr);
		settings = new SimulatorSettings();

		// Set up the logger
		logger = new Logger();

		this.engine = new SimulatorEngine();
		SimulatorMain.controller = new ConsoleController(this.engine);

		this.engine.resetEnvironment();

		if (!args.RC_FILE.equals("")) {
			SimulatorMain.controller.loadCommandFile(args.RC_FILE);
		}
		SimulatorMain.controller.start();


		if (!args.HEADLESS) {
			GUIDisplay gd = GUIDisplay.createInstance(this.engine);
			if (gd != null) {
				gd.setup();
				this.engine.setDisplay(gd);
			}
		}

		if (args.USE_AUTOSTART && !this.engine.isRunning()) {
			this.engine.runSimulation();
		}

	}


	public void registerControllerCommand(String cmdName, TerminalCommand cmd) {
		SimulatorMain.controller.registerCommand(cmdName, cmd);
	}


	public static void printStats(PrintStream ps) {
		ps.println("Name\tValue");

		ps.printf("Avg. covers per free cell\t%f\n", SimulatorMain.getStats().getAvgCoversPerFreeCell());
		ps.printf("Max covers of a cell\t%d\n", SimulatorMain.getStats().getMaxCellCovers());
		ps.printf("Min covers of a cell\t%d\n", SimulatorMain.getStats().getMinCellCovers());
		ps.printf("Number of cells covered exactly once\t%d\n", SimulatorMain.getStats().numFreeCellsCoveredNTimes(1));
		ps.printf("Total cells in grid\t%d\n", SimulatorMain.getStats().getTotalCells());
		ps.printf("Total non-obstacle cells\t%d\n", SimulatorMain.getStats().getTotalFreeCells());
		ps.printf("Total time steps\t%d\n", SimulatorMain.getStats().getNumTimeSteps());
		ps.printf("Robots broken\t%d\n", SimulatorMain.getStats().getNumBrokenRobots());
		ps.printf("Robots surviving\t%d\n", SimulatorMain.getStats().getNumSurvivingRobots());
		ps.printf("Percent covered\t%f\n", SimulatorMain.getStats().getFractionCovered() * 100.0);
		ps.printf("Best survivability\t%f\n", SimulatorMain.getStats().getMaxSurvivability());
		ps.printf("Best whole area coverage probability\t%.6E\n", SimulatorMain.getStats().getMaxCoverageProb());
	}


	public static void main(String[] argsArr) {
		new SimulatorMain(argsArr);
	}


	public static SimulationStats getStats() {
		return stats;
	}


	public static void setStats(SimulationStats stats) {
		SimulatorMain.stats = stats;
	}
}

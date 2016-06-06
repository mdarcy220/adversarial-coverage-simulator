package adversarialcoverage;

import java.io.PrintStream;
import coveragegui.GUIDisplay;

public class AdversarialCoverage {

	public static AdversarialCoverageArgs args = null;
	public static AdversarialCoverageSettings settings = null;
	public static Logger logger = null;
	private ConsoleController controller = null;

	GridEnvironment env = null;
	static SimulationStats stats;
	private CoverageEngine engine = null;


	public AdversarialCoverage(String argsArr[]) {
		// Set up args and settings first
		args = new AdversarialCoverageArgs(argsArr);
		settings = new AdversarialCoverageSettings();

		// Set up the logger
		logger = new Logger();

		this.engine = new CoverageEngine();
		this.engine.resetCoverageEnvironment();

		this.controller = new ConsoleController(this.engine);
		if (!args.RC_FILE.equals("")) {
			this.controller.loadCommandFile(args.RC_FILE);
		}
		this.controller.start();


		if (!args.HEADLESS) {
			GUIDisplay gd = new GUIDisplay(this.engine);
			gd.setup();
			this.engine.setDisplay(gd);
		}

		if (args.USE_AUTOSTART && !this.engine.isRunning()) {
			this.engine.runCoverage();
		}

	}


	public void registerControllerCommand(String cmdName, TerminalCommand cmd) {
		this.controller.registerCommand(cmdName, cmd);
	}


	public static void printStats(PrintStream ps) {
		ps.println("Name\tValue");

		ps.printf("Avg. covers per free cell\t%f\n", AdversarialCoverage.stats.getAvgCoversPerFreeCell());
		ps.printf("Max covers of a cell\t%d\n", AdversarialCoverage.stats.getMaxCellCovers());
		ps.printf("Min covers of a cell\t%d\n", AdversarialCoverage.stats.getMinCellCovers());
		ps.printf("Number of cells covered exactly once\t%d\n", AdversarialCoverage.stats.numFreeCellsCoveredNTimes(1));
		ps.printf("Total cells in grid\t%d\n", AdversarialCoverage.stats.getTotalCells());
		ps.printf("Total non-obstacle cells\t%d\n", AdversarialCoverage.stats.getTotalFreeCells());
		ps.printf("Total time steps\t%d\n", AdversarialCoverage.stats.getNumTimeSteps());
		ps.printf("Robots broken\t%d\n", AdversarialCoverage.stats.getNumBrokenRobots());
		ps.printf("Robots surviving\t%d\n", AdversarialCoverage.stats.getNumSurvivingRobots());
		ps.printf("Percent covered\t%f\n", AdversarialCoverage.stats.getFractionCovered() * 100.0);
		ps.printf("Best survivability\t%f\n", AdversarialCoverage.stats.getMaxSurvivability());
		ps.printf("Best whole area coverage probability\t%.6E\n", AdversarialCoverage.stats.getMaxCoverageProb());
	}


	public static void main(String[] argsArr) {
		new AdversarialCoverage(argsArr);
	}
}

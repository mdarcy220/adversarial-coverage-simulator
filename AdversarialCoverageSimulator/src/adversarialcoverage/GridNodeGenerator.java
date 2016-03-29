package adversarialcoverage;
import java.util.Random;
import java.util.Scanner;

public class GridNodeGenerator {
	// Generation parameters
	double dangerMin = 0.0;
	double dangerMax = 1.0;
	double obstacleProb = 0.33;
	double dangerNodeProb = 0.33;
	int coverCountStart = 0;
	int repeatCount = 0;
	String paramStr = "";
	boolean stringEmpty = true;
	private Scanner scan;
	Random randgen = new Random();


	/**
	 * Default constructor
	 */
	public GridNodeGenerator() {
		reset();
	}


	/**
	 * Resets parameters to default values
	 */
	public void reset() {
		this.randgen.setSeed(System.nanoTime());
		this.resetNodeParameters();
		this.paramStr = "";
		this.stringEmpty = true;
		this.scan = new Scanner(this.paramStr);
	}


	private void resetNodeParameters() {
		this.dangerMin = 0.0;
		this.dangerMax = 1.0;
		this.obstacleProb = 0.33;
		this.dangerNodeProb = 0.33;
		this.coverCountStart = 0;
		this.repeatCount = 0;
	}


	/**
	 * Uses the given {@code String} to fetch parameters for generating
	 * nodes, until no more parameter sets can be fetched. After exhausting
	 * the parameters from the string, the last parameters scanned will be
	 * used.
	 * 
	 * @param scan
	 */
	public void useScanner(String str) {
		this.stringEmpty = false;
		this.paramStr = str;
		this.scan = new Scanner(this.paramStr);
	}


	/**
	 * Checks if the scanner is still useable to get more parameters
	 * 
	 * @return
	 */
	public boolean stringEmpty() {
		return this.stringEmpty;
	}


	/**
	 * Sets the parameters of the given grid node using the parameters
	 * stored.
	 * 
	 * @return
	 */
	public void genNext(GridNode node) {
		if (this.repeatCount <= 0) {
			loadNextParameterSet();
		} else {
			this.repeatCount--;
		}

		double rand = this.randgen.nextDouble();
		if (rand < this.obstacleProb) {
			node.setNodeType(NodeType.OBSTACLE);
		} else if ((rand - this.obstacleProb) < this.dangerNodeProb) {
			node.setNodeType(NodeType.FREE);
			node.setDangerProb(
					this.randgen.nextDouble() * (this.dangerMax - this.dangerMin) + this.dangerMin);
		} else {
			node.setNodeType(NodeType.FREE);
			node.setDangerProb(0.0);
		}


		node.setCoverCount(this.coverCountStart);
	}


	/**
	 * If a parameter {@code String} is available, loads the next set of
	 * parameters from it.
	 */
	private void loadNextParameterSet() {
		if (!this.stringEmpty && this.scan.hasNext()) {
			this.resetNodeParameters();
		}
		while (!this.stringEmpty && loadNextParameter() == false) {
			; // Do nothing
		}
	}


	/**
	 * 
	 * @return true if a danger level has bee found (and this the end of the
	 *         parameter set has been reached), false otherwise
	 */
	private boolean loadNextParameter() {
		if (this.stringEmpty || !this.scan.hasNext()) {
			this.stringEmpty = true;
			return false;
		}
		String strVal = this.scan.next().trim();
		if (strVal.charAt(0) != '@') {
			if (this.scan.hasNextDouble()) {
				double nodeDangerLevel = this.scan.nextDouble();
				this.dangerMax = nodeDangerLevel;
				this.dangerMin = nodeDangerLevel;
				this.dangerNodeProb = 1.0;
				this.obstacleProb = 0.0;
			} else {
				System.err.println("Error.");
			}
			return true;
		}
		if (strVal.length() < 2) {
			System.err.println("Problem parsing input.");
			this.stringEmpty = true;
			return false;
		}

		char code = strVal.charAt(1);
		if (code == 'r') {
			if (3 <= strVal.length() && strVal.charAt(2) == 'o' && this.scan.hasNextDouble()) {
				this.obstacleProb = this.scan.nextDouble();
			}
			if (4 <= strVal.length() && strVal.charAt(3) == 'd' && this.scan.hasNextDouble()) {
				this.dangerNodeProb = this.scan.nextDouble();
			}
			if (this.scan.hasNextDouble()) {
				this.dangerMin = this.scan.nextDouble();
			}
			if (this.scan.hasNextDouble()) {
				this.dangerMax = this.scan.nextDouble();
			} else {
				this.stringEmpty = true;
			}
			return true;
		} else if (code == 'o') {
			if (this.scan.hasNextDouble()) {
				this.obstacleProb = this.scan.nextDouble();
			}
		} else if (code == 'm') {
			if (this.scan.hasNextInt()) {
				this.repeatCount = this.scan.nextInt();
			} else {
				this.stringEmpty = true;
			}
		} else if (code == 'd') {
			if (this.scan.hasNextDouble()) {
				this.dangerNodeProb = this.scan.nextDouble();
			}
		}
		return false;
	}
}

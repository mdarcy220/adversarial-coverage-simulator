package adversarialcoverage;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

public class GridNodeGenerator {

	private Random randgen = new Random();

	private List<List<GridNodeTemplate>> maps = new ArrayList<>();
	private int mapNum = 0;
	private int mapNodeNum = 0;
	private int timesRepeated = 0;

	private String genStr = "";


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
		this.genStr = "";
		this.compileParamsFromString(this.genStr);
	}


	public String getGeneratorString() {
		return this.genStr;
	}


	private void resetNodeParameters() {
		this.timesRepeated = 0;
	}


	public void setGeneratorString(String genStr) {
		this.genStr = genStr;
		this.compileParamsFromString(this.genStr);
	}


	private void compileParamsFromString(String paramStr) {
		this.maps.clear();

		Scanner paramScanner = new Scanner(paramStr);
		List<GridNodeTemplate> curMap = compileMapFromScanner(paramScanner);

		while (curMap != null) {
			this.maps.add(curMap);
			curMap = compileMapFromScanner(paramScanner);
		}

		// if no input is given, default to a generic all-free map
		if (this.maps.size() == 0) {
			curMap = new ArrayList<>();
			curMap.add(new GridNodeTemplate());
			this.maps.add(curMap);
		}

		paramScanner.close();
	}


	private List<GridNodeTemplate> compileMapFromScanner(Scanner paramScanner) {
		if (!paramScanner.hasNext()) {
			return null;
		}

		List<GridNodeTemplate> curMap = new ArrayList<>();
		GridNodeTemplate curNode = compileGridNodeTemplateFromScanner(paramScanner);

		while (curNode != null && !curNode.isLastInMap) {
			curMap.add(curNode);
			curNode = compileGridNodeTemplateFromScanner(paramScanner);
		}

		if (curNode != null && curNode.isLastInMap) {
			curMap.add(curNode);
		}


		return curMap;
	}


	private GridNodeTemplate compileGridNodeTemplateFromScanner(Scanner paramScanner) {
		if (!paramScanner.hasNext()) {
			return null;
		}
		GridNodeTemplate curNode = new GridNodeTemplate();
		boolean hasAllParams = false;
		while (!hasAllParams) {
			hasAllParams = loadNextTemplateParam(paramScanner, curNode);
		}

		return curNode;
	}


	private boolean loadNextTemplateParam(Scanner paramScanner, GridNodeTemplate curNode) {
		String strVal = paramScanner.next().trim();
		if (strVal.charAt(0) != '@') {
			boolean hasError = false;
			double nodeDangerLevel = 0.0;
			try {
				nodeDangerLevel = Double.parseDouble(strVal);
			} catch (NumberFormatException e) {
				hasError = true;
			}
			if (!hasError) {
				curNode.dangerMax = nodeDangerLevel;
				curNode.dangerMin = nodeDangerLevel;
				curNode.dangerNodeProb = 1.0;
				curNode.obstacleProb = 0.0;
			} else {
				System.err.println("Error parsing grid generator string.");
			}
			return true;
		}
		if (strVal.length() < 2) {
			System.err.println("Problem parsing input.");
			return false;
		}

		char code = strVal.charAt(1);
		if (code == 'r') {

			if (3 <= strVal.length() && strVal.charAt(2) == 'o' && paramScanner.hasNextDouble()) {
				curNode.obstacleProb = paramScanner.nextDouble();
			}
			if (4 <= strVal.length() && strVal.charAt(3) == 'd' && paramScanner.hasNextDouble()) {
				curNode.dangerNodeProb = paramScanner.nextDouble();
			}
			if (paramScanner.hasNextDouble()) {
				curNode.dangerMin = paramScanner.nextDouble();
			}
			if (paramScanner.hasNextDouble()) {
				curNode.dangerMax = paramScanner.nextDouble();
			}
			return true;

		} else if (code == 'o' && paramScanner.hasNextDouble()) {
			curNode.obstacleProb = paramScanner.nextDouble();
		} else if (code == 'm' && paramScanner.hasNextInt()) {
			curNode.repeatCount = paramScanner.nextInt();
		} else if (code == 'd' && paramScanner.hasNextDouble()) {
			curNode.dangerNodeProb = paramScanner.nextDouble();
		}

		if (strVal.equals("@lastInMap")) {
			curNode.isLastInMap = true;
		}

		return false;
	}


	public void setRandomMap() {
		this.mapNum = this.randgen.nextInt(this.maps.size());
		this.mapNodeNum = 0;
		this.timesRepeated = 0;
	}


	/**
	 * Sets the parameters of the given grid node using the parameters stored.
	 * 
	 * @return
	 */
	public void genNext(GridNode node) {
		GridNodeTemplate curNode = this.maps.get(this.mapNum).get(this.mapNodeNum);
		curNode.applyToGridNode(node, this.randgen);
		this.timesRepeated++;
		if (curNode.repeatCount <= this.timesRepeated) {
			if ((this.mapNodeNum + 1) < this.maps.get(this.mapNum).size()) {
				this.mapNodeNum++;
			}
			this.timesRepeated = 0;
		}
	}

}


class GridNodeTemplate {
	public double dangerMin = 0.0;
	public double dangerMax = 0.0;
	public double dangerNodeProb = 0.0;
	public double obstacleProb = 0.0;

	public int coverCountStart = 0;
	public int repeatCount = 1;

	public boolean isLastInMap = false;


	public GridNodeTemplate() {

	}


	/**
	 * Applies this template to the given grid node.
	 * 
	 * @param node
	 *                the grid node to apply this template to
	 * @param randgen
	 *                a random number source to use for evaluating probabilities
	 */
	public void applyToGridNode(GridNode node, Random randgen) {
		double rand = randgen.nextDouble();
		if (rand < this.obstacleProb) {
			node.setNodeType(NodeType.OBSTACLE);
		} else if ((rand - this.obstacleProb) < this.dangerNodeProb) {
			node.setNodeType(NodeType.FREE);
			node.setDangerProb(randgen.nextDouble() * (this.dangerMax - this.dangerMin) + this.dangerMin);
		} else {
			node.setNodeType(NodeType.FREE);
			node.setDangerProb(0.0);
		}


		node.setCoverCount(this.coverCountStart);
	}
}

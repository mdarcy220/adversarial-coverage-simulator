package deeplearning;

import adversarialcoverage.AdversarialCoverage;
import adversarialcoverage.GridSensor;

public class StatePreprocessor {

	final boolean GIVE_GLOBAL_POS_AND_SIZE = AdversarialCoverage.settings.getBooleanProperty("neuralnet.give_global_pos_and_size");
	int VISION_SIZE = AdversarialCoverage.settings.getIntProperty("deepql.nn_input.vision_radius");
	boolean NN_INPUT_OBSTACLE_LAYER = AdversarialCoverage.settings.getBooleanProperty("deepql.nn_input.obstacle_layer");
	final int NN_INPUT_SIZE = calcStateSize();
	VisionType visiontype = VisionType.CENTERED_SNAP_TO_EDGES;
	boolean attemptToNormalize = true;
	GridSensor sensor;


	public StatePreprocessor(GridSensor sensor) {
		this.sensor = sensor;
	}


	public double[] getPreprocessedState() {
		switch (this.visiontype) {
		case CENTERED_ALWAYS:
			return null;
		case CENTERED_SNAP_TO_EDGES: // FALLTHROUGH
		default:
			return getPreprocessedState_centered_snap_to_edges();
		}
	}


	private double[] getPreprocessedState_centered_snap_to_edges() {

		double[] curState = new double[this.NN_INPUT_SIZE];
		int xLowBound = Math.min(Math.max(this.sensor.getLocation().x - this.VISION_SIZE + 2, 0),
				(this.sensor.getGridWidth() - this.VISION_SIZE));
		int yLowBound = Math.min(Math.max(this.sensor.getLocation().y - this.VISION_SIZE + 2, 0),
				(this.sensor.getGridHeight() - this.VISION_SIZE));
		int layerSize = this.VISION_SIZE * this.VISION_SIZE;
		for (int x = 0; x < this.VISION_SIZE; x++) {
			for (int y = 0; y < this.VISION_SIZE; y++) {
				int cellNum = x * this.VISION_SIZE + y;
				// Danger level layer
				curState[cellNum] = this.sensor.getDangerLevelAt(xLowBound + x, yLowBound + y) * 3.0;

				// Cover count layer
				curState[layerSize + cellNum] = this.sensor.getCoverCountAt(xLowBound + x, yLowBound + y) < 1 ? -1 : 1;

				// Robot position layer
				curState[(2 * layerSize) + cellNum] = 0;

				// Obstacle layer
				if (this.NN_INPUT_OBSTACLE_LAYER) {
					curState[(3 * layerSize) + cellNum] = this.sensor.isObstacle(xLowBound + x, yLowBound + y) ? 1.0 : -1.0;
				}
			}
		}
		curState[50 + (this.sensor.getLocation().x - xLowBound) * 5 + (this.sensor.getLocation().y - yLowBound)] = 1;
		if (this.GIVE_GLOBAL_POS_AND_SIZE) {
			curState[this.NN_INPUT_SIZE - 4] = this.sensor.getLocation().x;
			curState[this.NN_INPUT_SIZE - 3] = this.sensor.getLocation().y;
			curState[this.NN_INPUT_SIZE - 2] = this.sensor.getGridWidth();
			curState[this.NN_INPUT_SIZE - 1] = this.sensor.getGridHeight();
		}

		if (this.attemptToNormalize) {
			// Normalize the data
			// The sum of squares should be around 27 (1 for danger levels +
			// 25 for coverage + 1 for location)
			double approxStdDev = Math.sqrt(27.0);
			for (int i = 0; i < this.NN_INPUT_SIZE; i++) {
				curState[i] /= approxStdDev;
			}
		}

		return curState;
	}


	private int calcStateSize() {
		int layersize = this.VISION_SIZE * this.VISION_SIZE;
		int numLayers = 3 + (AdversarialCoverage.settings.getBooleanProperty("deepql.nn_input.obstacle_layer") ? 1 : 0);
		int miscInputs = (AdversarialCoverage.settings.getBooleanProperty("neuralnet.give_global_pos_and_size") ? 4 : 0);

		return (layersize * numLayers) + miscInputs;
	}


	public void reloadSettings() {
		this.sensor.reloadSettings();
	}


	enum VisionType {
		CENTERED_ALWAYS, CENTERED_SNAP_TO_EDGES,
	}
}

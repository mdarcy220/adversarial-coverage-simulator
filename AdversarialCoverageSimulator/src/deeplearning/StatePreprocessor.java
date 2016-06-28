package deeplearning;

import adversarialcoverage.AdversarialCoverage;
import adversarialcoverage.GridSensor;

public class StatePreprocessor {

	private final boolean GIVE_GLOBAL_POS_AND_SIZE = AdversarialCoverage.settings.getBoolean("neuralnet.give_global_pos_and_size");
	private final int VISION_SIZE = AdversarialCoverage.settings.getInt("deepql.nn_input.vision_radius");
	private boolean NN_INPUT_OBSTACLE_LAYER = AdversarialCoverage.settings.getBoolean("deepql.nn_input.obstacle_layer");
	private final int NN_INPUT_SIZE = calcStateSize();
	private VisionType visiontype = VisionType.CENTERED_SNAP_TO_EDGES;
	private GridSensor sensor;


	public StatePreprocessor(GridSensor sensor) {
		this.sensor = sensor;
	}


	public double[] getPreprocessedState() {
		return this.getPreprocessedState(new double[this.NN_INPUT_SIZE]);
	}


	public double[] getPreprocessedState(double[] stateBuffer) {
		switch (this.visiontype) {
		case CENTERED_ALWAYS:
			return null;
		case CENTERED_SNAP_TO_EDGES: // FALLTHROUGH
		default:
			return getPreprocessedState_centered_snap_to_edges(stateBuffer);
		}
	}


	private double[] getPreprocessedState_centered_snap_to_edges(double[] stateBuf) {
		// Ordering of statements is very important for these lowBound variables.
		// We need them to be 0 if the vision size is greater than the grid size
		int xLowBound = Math.min(this.sensor.getGridWidth() - 1, this.sensor.getLocation().x + (this.VISION_SIZE / 2))
				- (this.VISION_SIZE - 1);
		xLowBound = Math.max(xLowBound, 0);
		int yLowBound = Math.min(this.sensor.getGridHeight() - 1, this.sensor.getLocation().y + (this.VISION_SIZE / 2))
				- (this.VISION_SIZE - 1);
		yLowBound = Math.max(yLowBound, 0);
		int layerSize = this.VISION_SIZE * this.VISION_SIZE;

		for (int x = 0; x < this.VISION_SIZE; x++) {
			for (int y = 0; y < this.VISION_SIZE; y++) {
				int cellNum = x * this.VISION_SIZE + y;
				int gridX = xLowBound + x;
				int gridY = yLowBound + y;
				boolean cellExists = this.sensor.nodeExists(gridX, gridY);
				if (cellExists) {
					// Danger level layer
					stateBuf[cellNum] = this.sensor.getDangerLevelAt(gridX, gridY) * 3.0;

					// Cover count layer
					stateBuf[layerSize + cellNum] = this.sensor.getCoverCountAt(gridX, gridY) < 1 ? -1 : 1;

					// Robot position layer
					stateBuf[(2 * layerSize) + cellNum] = 0;

					// Obstacle layer
					if (this.NN_INPUT_OBSTACLE_LAYER) {
						stateBuf[(3 * layerSize) + cellNum] = this.sensor.isObstacle(gridX, gridY) ? 1.0 : -1.0;
					}
				} else {
					// Danger level layer
					stateBuf[cellNum] = 0;

					// Cover count layer
					stateBuf[layerSize + cellNum] = 0;

					// Robot position layer
					stateBuf[(2 * layerSize) + cellNum] = 0;

					// Obstacle layer
					if (this.NN_INPUT_OBSTACLE_LAYER) {
						stateBuf[(3 * layerSize) + cellNum] = 1.0;
					}
				}
			}
		}

		stateBuf[(2 * layerSize) + ((this.sensor.getLocation().x - xLowBound) * this.VISION_SIZE)
				+ (this.sensor.getLocation().y - yLowBound)] = 1;
		if (this.GIVE_GLOBAL_POS_AND_SIZE) {
			stateBuf[this.NN_INPUT_SIZE - 4] = this.sensor.getLocation().x;
			stateBuf[this.NN_INPUT_SIZE - 3] = this.sensor.getLocation().y;
			stateBuf[this.NN_INPUT_SIZE - 2] = this.sensor.getGridWidth();
			stateBuf[this.NN_INPUT_SIZE - 1] = this.sensor.getGridHeight();
		}

		if (AdversarialCoverage.settings.getBoolean("deepql.statepreprocessor.attempt_mormalization")) {
			// Normalize the data
			// The sum of squares should be around 27 (1 for danger levels +
			// 25 for coverage + 1 for location)
			double approxStdDev = Math.sqrt(27.0);
			for (int i = 0; i < this.NN_INPUT_SIZE; i++) {
				stateBuf[i] /= approxStdDev;
			}
		}

		return stateBuf;
	}


	public double[] createEmptyStateBuffer() {
		return new double[this.NN_INPUT_SIZE];
	}


	private int calcStateSize() {
		int layersize = this.VISION_SIZE * this.VISION_SIZE;
		int numLayers = 3 + (AdversarialCoverage.settings.getBoolean("deepql.nn_input.obstacle_layer") ? 1 : 0);
		int miscInputs = (AdversarialCoverage.settings.getBoolean("neuralnet.give_global_pos_and_size") ? 4 : 0);

		return (layersize * numLayers) + miscInputs;


	}


	public int getNNInputSize() {
		return this.NN_INPUT_SIZE;
	}


	public void reloadSettings() {
		this.sensor.reloadSettings();
	}


	enum VisionType {
		CENTERED_ALWAYS, CENTERED_SNAP_TO_EDGES,
	}
}

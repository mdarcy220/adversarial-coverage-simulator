package simulations.coverage;

import adsim.SettingsReloadable;
import adsim.SimulatorMain;
import deeplearning.DQLStatePreprocessor;
import gridenv.GridSensor;

public class CoverageStatePreprocessor implements DQLStatePreprocessor, SettingsReloadable {

	private final boolean GIVE_GLOBAL_POS_AND_SIZE = SimulatorMain.settings.getBoolean("neuralnet.give_global_pos_and_size");
	private final int VISION_SIZE = SimulatorMain.settings.getInt("deepql.nn_input.vision_radius");
	private boolean NN_INPUT_OBSTACLE_LAYER = SimulatorMain.settings.getBoolean("deepql.nn_input.obstacle_layer");
	private final int NN_INPUT_SIZE;
	private boolean ATTEMPT_NORMALIZATION = SimulatorMain.settings.getBoolean("deepql.statepreprocessor.attempt_mormalization");
	private double OUT_OF_BOUNDS_VALS_DANGER = SimulatorMain.settings.getDouble("deepql.statepreprocessor.out_of_bounds_vals.danger");
	private double OUT_OF_BOUNDS_VALS_COVER = SimulatorMain.settings.getDouble("deepql.statepreprocessor.out_of_bounds_vals.cover");
	private double OUT_OF_BOUNDS_VALS_OBSTACLE = SimulatorMain.settings.getDouble("deepql.statepreprocessor.out_of_bounds_vals.obstacle");
	private VisionType visiontype;
	private GridSensor sensor;


	public CoverageStatePreprocessor(GridSensor sensor) {
		this.sensor = sensor;
		this.NN_INPUT_SIZE = calcStateSize();
	}


	@Override
	public double[] getPreprocessedState() {
		return this.getPreprocessedState(new double[this.NN_INPUT_SIZE]);
	}


	@Override
	public double[] getPreprocessedState(double[] stateBuffer) {
		if (stateBuffer == null || stateBuffer.length < this.NN_INPUT_SIZE) {
			return this.getPreprocessedState();
		}
		switch (this.visiontype) {
		case CENTERED_ALWAYS:
			return getPreprocessedState_centered_always(stateBuffer);
		case CENTERED_SNAP_TO_EDGES: // FALLTHROUGH TO DEFAULT
		default:
			return getPreprocessedState_centered_snap_to_edges(stateBuffer);
		}
	}


	private double[] getPreprocessedState_centered_snap_to_edges(double[] stateBuf) {
		// Ordering of statements is very important for these lowBound variables.
		// We need them to be 0 if the vision size is greater than the grid size
		int xLowBound = Math.min(this.sensor.getGridWidth() - 1, this.sensor.getX() + (this.VISION_SIZE / 2)) - (this.VISION_SIZE - 1);
		xLowBound = Math.max(xLowBound, 0);
		int yLowBound = Math.min(this.sensor.getGridHeight() - 1, this.sensor.getY() + (this.VISION_SIZE / 2)) - (this.VISION_SIZE - 1);
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
					stateBuf[cellNum] = this.OUT_OF_BOUNDS_VALS_DANGER;

					// Cover count layer
					stateBuf[layerSize + cellNum] = this.OUT_OF_BOUNDS_VALS_COVER;

					// Robot position layer
					stateBuf[(2 * layerSize) + cellNum] = 0;

					// Obstacle layer
					if (this.NN_INPUT_OBSTACLE_LAYER) {
						stateBuf[(3 * layerSize) + cellNum] = this.OUT_OF_BOUNDS_VALS_OBSTACLE;
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

		if (this.ATTEMPT_NORMALIZATION) {
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


	private double[] getPreprocessedState_centered_always(double[] stateBuf) {

		// Setting the grid coordinates of the upper-left corner of the vision
		// For the centering of even-numbered vision sizes, they should work
		// identically to the odd-numbered size one below them, so for example 5
		// and 6 should give the same lower bounds for x and y.
		int xLowBound = this.sensor.getX() - ((this.VISION_SIZE - 1) / 2);
		int yLowBound = this.sensor.getY() - ((this.VISION_SIZE - 1) / 2);

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
					stateBuf[layerSize + cellNum] = this.sensor.getCoverCountAt(gridX, gridY) < 1 ? -1.0 : 1.0;

					// Obstacle layer
					if (this.NN_INPUT_OBSTACLE_LAYER) {
						stateBuf[(2 * layerSize) + cellNum] = this.sensor.isObstacle(gridX, gridY) ? 1.0 : -1.0;
					}
				} else {
					// Danger level layer
					stateBuf[cellNum] = this.OUT_OF_BOUNDS_VALS_DANGER;

					// Cover count layer
					stateBuf[layerSize + cellNum] = this.OUT_OF_BOUNDS_VALS_COVER;

					// Obstacle layer
					if (this.NN_INPUT_OBSTACLE_LAYER) {
						stateBuf[(2 * layerSize) + cellNum] = this.OUT_OF_BOUNDS_VALS_OBSTACLE;
					}
				}
			}
		}

		if (this.GIVE_GLOBAL_POS_AND_SIZE) {
			stateBuf[this.NN_INPUT_SIZE - 4] = this.sensor.getLocation().x;
			stateBuf[this.NN_INPUT_SIZE - 3] = this.sensor.getLocation().y;
			stateBuf[this.NN_INPUT_SIZE - 2] = this.sensor.getGridWidth();
			stateBuf[this.NN_INPUT_SIZE - 1] = this.sensor.getGridHeight();
		}

		if (this.ATTEMPT_NORMALIZATION) {
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
		this.reloadSettings();

		int layersize = this.VISION_SIZE * this.VISION_SIZE;
		int numLayers = 2;
		if (this.NN_INPUT_OBSTACLE_LAYER) {
			numLayers += 1;
		}
		if (this.visiontype == VisionType.CENTERED_SNAP_TO_EDGES) {
			numLayers += 1;
		}
		int miscInputs = (this.GIVE_GLOBAL_POS_AND_SIZE ? 4 : 0);

		return (layersize * numLayers) + miscInputs;


	}


	@Override
	public int getStateSize() {
		return this.NN_INPUT_SIZE;
	}


	@Override
	public void reloadSettings() {
		this.sensor.reloadSettings();
		try {
			this.visiontype = VisionType.valueOf(SimulatorMain.settings.getString("deepql.statepreprocessor.vision_type"));
		} catch (IllegalArgumentException e) {
			System.err.println("Could not set vision type. No such enum value exists.");
		}
		this.ATTEMPT_NORMALIZATION = SimulatorMain.settings.getBoolean("deepql.statepreprocessor.attempt_mormalization");
		this.NN_INPUT_OBSTACLE_LAYER = SimulatorMain.settings.getBoolean("deepql.nn_input.obstacle_layer");
		this.OUT_OF_BOUNDS_VALS_DANGER = SimulatorMain.settings.getDouble("deepql.statepreprocessor.out_of_bounds_vals.danger");
		this.OUT_OF_BOUNDS_VALS_COVER = SimulatorMain.settings.getDouble("deepql.statepreprocessor.out_of_bounds_vals.cover");
		this.OUT_OF_BOUNDS_VALS_OBSTACLE = SimulatorMain.settings.getDouble("deepql.statepreprocessor.out_of_bounds_vals.obstacle");
	}


	enum VisionType {
		CENTERED_ALWAYS, CENTERED_SNAP_TO_EDGES;
	}
}

package adversarialcoverage;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Random;
import java.util.Scanner;

import deeplearning.NeuralNet;
import deeplearning.StatePreprocessor;
import deeplearning.NeuralNet.TrainingType;
import deeplearning.ActivationFunction;
import deeplearning.ExternalTorchNN;

public class DeepQLGridCoverage extends CoverageAlgorithm {
	NeuralNet nn = null;
	GridSensor sensor;
	GridActuator actuator;
	Random randgen = new Random();
	StatePreprocessor preprocessor;
	double greedyEpsilon = AdversarialCoverage.settings.getDoubleProperty("deepql.greedy_epsilon_start");
	long stepNum = 0;
	StateTransition[] lastStates;
	int VISION_RADIUS = AdversarialCoverage.settings.getIntProperty("deepql.nn_input.vision_radius");
	boolean NN_INPUT_OBSTACLE_LAYER = AdversarialCoverage.settings.getBooleanProperty("deepql.nn_input.obstacle_layer");
	final int NN_INPUT_SIZE = calcStateSize();
	int HISTORY_MAX = AdversarialCoverage.settings.getIntProperty("deepql.history_max");
	double DISCOUNT_FACTOR = AdversarialCoverage.settings.getDoubleProperty("deepql.discountfactor");
	double GREEDY_EPSILON_DECREMENT = AdversarialCoverage.settings.getDoubleProperty("deepql.greedy_epsilon_decrement");
	double GREEDY_EPSILON_MINIMUM = AdversarialCoverage.settings.getDoubleProperty("deepql.greedy_epsilon_minimum");
	int MINIBATCH_SIZE = AdversarialCoverage.settings.getIntProperty("deepql.minibatch_size");
	double LEARNING_RATE_DECAY_FACTOR = AdversarialCoverage.settings.getDoubleProperty("deepql.learning_rate_decay_factor");
	TrainingType NN_TRAINING_TYPE = TrainingType.RMSPROP;
	boolean PRINT_Q_VALUES = AdversarialCoverage.settings.getBooleanProperty("deepql.display.print_q_values");
	int HIDDEN_LAYER_SIZE = AdversarialCoverage.settings.getIntProperty("neuralnet.hidden_layer_size");
	int NUM_HIDDEN_LAYERS = AdversarialCoverage.settings.getIntProperty("neuralnet.num_hidden_layers");
	final boolean GIVE_GLOBAL_POS_AND_SIZE = AdversarialCoverage.settings.getBooleanProperty("neuralnet.give_global_pos_and_size");
	boolean USING_EXTERNAL_QLEARNER = AdversarialCoverage.settings.getStringProperty("deepql.nn_setup_mode").equalsIgnoreCase("torch")
			&& AdversarialCoverage.settings.getBooleanProperty("deepql.use_external_qlearner");
	long stateHistorySize = 0;


	private int calcStateSize() {
		int layersize = this.VISION_RADIUS * this.VISION_RADIUS;
		int numLayers = 3 + (AdversarialCoverage.settings.getBooleanProperty("deepql.nn_input.obstacle_layer") ? 1 : 0);
		int miscInputs = (AdversarialCoverage.settings.getBooleanProperty("neuralnet.give_global_pos_and_size") ? 4 : 0);

		return (layersize * numLayers) + miscInputs;
	}


	public DeepQLGridCoverage(GridSensor sensor, GridActuator actuator) {
		this.sensor = sensor;
		this.actuator = actuator;
		this.preprocessor = new StatePreprocessor(this.sensor);
		this.lastStates = new StateTransition[this.HISTORY_MAX];
	}


	@Override
	public void init() {
		if (this.nn == null) {
			this.reloadSettings();

			initNeuralNet();
			printNeuralNetParams();
		}
	}


	private void printNeuralNetParams() {
		System.out.printf("LEARNING_RATE=%f\n", this.nn.LEARNING_RATE);
		System.out.printf("MOMENTUM_FACTOR=%f\n", this.nn.MOMENTUM_GAMMA);
		System.out.printf("GREEDINESS=%f\n", (1.0 - this.greedyEpsilon));
		System.out.printf("GREEDINESS_INCREMENT=%E\n", this.GREEDY_EPSILON_DECREMENT);
		System.out.printf("GREEDINESS_MAX=%E\n", 1.0 - this.GREEDY_EPSILON_MINIMUM);
		System.out.printf("MINIBATCH_SIZE=%d\n", this.MINIBATCH_SIZE);
		System.out.printf("DISCOUNT_FACTOR=%f\n", this.DISCOUNT_FACTOR);
		System.out.printf("STATE_SIZE=%d\n", this.NN_INPUT_SIZE);
		System.out.printf("HISTORY_MAX=%d\n", this.HISTORY_MAX);
		System.out.printf("ROBOTS_BREAKABLE=%b\n", AdversarialCoverage.settings.getBooleanProperty("robots.breakable"));
		System.out.printf("GRID_GENERATOR_STRING=%s\n", AdversarialCoverage.settings.getStringProperty("env.grid.dangervalues"));

		System.out.printf("NEURAL_NET_LAYERS=[");
		int[] layerSizes = this.nn.getLayerSizes();
		for (int i = 0; i < layerSizes.length - 1; i++) {
			System.out.printf("%d, ", layerSizes[i]);
		}
		if (1 <= layerSizes.length) {
			System.out.printf("%d", layerSizes[layerSizes.length - 1]);
		}
		System.out.printf("]\n");

		System.out.printf("NEURAL_NET_TRAINER=%s\n", this.nn.trainingType == TrainingType.RMSPROP ? "RMSProp" : "Momentum");

		System.out.println("----------------");
	}


	@Override
	public void step() {
		double[] curState = this.preprocessor.getPreprocessedState();
		double[] nnInput = new double[this.NN_INPUT_SIZE];

		for (int i = 0; i < this.NN_INPUT_SIZE; i++) {
			nnInput[i] = curState[i];
		}


		StateTransition transition = new StateTransition(nnInput);
		this.nn.feedForward(nnInput);
		double[] nnOutput = this.nn.getOutputs();
		if (this.PRINT_Q_VALUES) {
			printQVals(nnOutput);
		}
		double maxVal = Double.NEGATIVE_INFINITY;
		int maxIndex = 0;
		for (int i = 0; i < nnOutput.length; i++) {
			if (maxVal < nnOutput[i]) {
				maxVal = nnOutput[i];
				maxIndex = i;
			}
		}
		int action = maxIndex;
		if (this.randgen.nextDouble() < this.greedyEpsilon) {
			action = this.randgen.nextInt(5);
		}
		transition.action = action;
		takeAction(action);
		curState = this.preprocessor.getPreprocessedState();

		double[] nnInput2 = new double[this.NN_INPUT_SIZE];
		for (int i = 0; i < this.NN_INPUT_SIZE; i++) {
			nnInput2[i] = curState[i];
		}
		transition.nextInput = curState;

		transition.reward = this.actuator.getLastReward();
		transition.correctQVal = transition.reward;
		transition.isTerminal = this.sensor.env.isFinished();

		if (this.stepNum % 500 == 0) {
			if (!this.sensor.env.isFinished()) {
				this.nn.feedForward(curState);
				double[] nnOutput2 = this.nn.getOutputs();
				maxVal = Double.NEGATIVE_INFINITY;
				for (int i = 0; i < nnOutput2.length; i++) {
					if (maxVal < nnOutput2[i]) {
						maxVal = nnOutput2[i];
					}
				}

				transition.correctQVal += this.DISCOUNT_FACTOR * maxVal;
			}
			if (this.PRINT_Q_VALUES) {
				System.out.printf("\"correct\" q-value for %d: %f\n", action, transition.correctQVal);
			}

			double squaredErrorSum = this.getSquaredErrorSum(transition.correctQVal, nnOutput[action]);
			System.out.printf("Loss for minibatch %d: %f  (%.6f)\n", this.stepNum, transition.correctQVal - nnOutput[action],
					squaredErrorSum);
		}
		
		if (!this.USING_EXTERNAL_QLEARNER) {
			this.lastStates[(int) (this.stepNum % this.HISTORY_MAX)] = transition;
			this.stateHistorySize = Math.min(this.HISTORY_MAX, this.stepNum);
		}

		if (this.USING_EXTERNAL_QLEARNER && this.nn instanceof ExternalTorchNN) {
			((ExternalTorchNN) this.nn).sendTransition(transition);
			((ExternalTorchNN) this.nn).runTorchMinibatch();
		} else if (this.MINIBATCH_SIZE < this.stepNum) {
			trainMinibatchFromHistory(this.MINIBATCH_SIZE);
		}

		if (this.stepNum % 25000 == 0) {
			this.nn.LEARNING_RATE *= this.LEARNING_RATE_DECAY_FACTOR;
			System.out.println(this.nn.exportToString());
			System.out.println("Minibatch number=" + this.stepNum);
			System.out.println("Epsilon=" + this.greedyEpsilon);
			System.out.println("Learning rate=" + this.nn.LEARNING_RATE);
		}
		if (this.GREEDY_EPSILON_MINIMUM < this.greedyEpsilon) {
			this.greedyEpsilon -= this.GREEDY_EPSILON_DECREMENT;
		} else {
			this.greedyEpsilon = this.GREEDY_EPSILON_MINIMUM;
		}


		this.stepNum++;
	}


	private void printQVals(double[] nnOutput) {
		System.out.printf("estimated q-values: {east=%f, south=%f, west=%f, north=%f, stay=%f}\n", nnOutput[0], nnOutput[1], nnOutput[2],
				nnOutput[3], nnOutput[4]);
	}


	private void trainMinibatchFromHistory(int batchSize) {
		for (int i = 0; i < batchSize; i++) {
			int sampleNum = this.randgen.nextInt((int) this.stateHistorySize);
			StateTransition sample = this.lastStates[sampleNum];// this.lastStates.get(sampleNum);
			this.nn.feedForward(sample.nextInput);
			double nextQVal = fastMax_DoubleArr5(this.nn.getOutputs());
			this.nn.feedForward(sample.nnInput);
			double[] nnOutput = this.nn.getOutputs();
			double[] correctOut = new double[5];
			for (int j = 0; j < nnOutput.length; j++) {
				correctOut[j] = nnOutput[j];
			}
			correctOut[sample.action] = (sample.reward + (sample.isTerminal ? 0.0 : (this.DISCOUNT_FACTOR * nextQVal)));
			if (this.nn.trainingType == TrainingType.RMSPROP) {
				this.nn.backPropagateFromLastSample_RMSProp(correctOut);
			} else {
				this.nn.backPropagateFromLastSample_Momentum(correctOut);
			}
		}
		if (this.nn.trainingType == TrainingType.RMSPROP) {
			this.nn.finishBatch_RMSProp();
		} else {
			this.nn.finishBatch_Momentum();
		}

	}


	/**
	 * Quickly gets the value of the largest element in an array of 5 double values.
	 * 
	 * @return the largest double value in the 5-element array
	 */
	private double fastMax_DoubleArr5(double[] arr) {
		double maxVal = arr[0];
		if (maxVal < arr[1]) {
			maxVal = arr[1];
		}
		if (maxVal < arr[2]) {
			maxVal = arr[2];
		}
		if (maxVal < arr[3]) {
			maxVal = arr[3];
		}
		if (maxVal < arr[4]) {
			maxVal = arr[4];
		}
		return maxVal;
	}


	// Gets the sum of squared errors of the most recent output of the NN
	private double getSquaredErrorSum(double correctOutput, double givenOutput) {
		return (correctOutput - givenOutput) * (correctOutput - givenOutput);
	}


	private void initNeuralNet() {
		final String setupMode = AdversarialCoverage.settings.getStringProperty("deepql.nn_setup_mode");
		if (setupMode.equalsIgnoreCase("native")) {
			this.nn = new NeuralNet(new int[] { this.NN_INPUT_SIZE, 1 });
			this.nn.removeLastLayer();
			for (int i = 0; i < this.NUM_HIDDEN_LAYERS; i++) {
				this.nn.addFullyConnectedLayer(this.HIDDEN_LAYER_SIZE, ActivationFunction.RELU_ACTIVATION);
			}
			this.nn.addFullyConnectedLayer(5, ActivationFunction.LINEAR_ACTIVATION);
			final int[] nnLayerSizes = this.nn.getLayerSizes();
			this.nn.removeNeuronFromLayer(nnLayerSizes.length - 1, nnLayerSizes[nnLayerSizes.length - 1] - 1);
			this.nn.trainingType = this.NN_TRAINING_TYPE;
		} else if (setupMode.equalsIgnoreCase("torch")) {
			String prefix = AdversarialCoverage.settings.getStringProperty("deepql.external_torch_nn.io_file_prefix");
			this.nn = new ExternalTorchNN(prefix + "input2.dat", prefix + "output2.dat");
			System.out.println("Using Torch neural network...");
		} else {

			try {
				Scanner scan = new Scanner(new File(AdversarialCoverage.settings.getStringProperty("neuralnet.loadfile")));
				this.nn = new NeuralNet(scan.nextLine());
				int numLayers = this.nn.getLayerSizes().length;
				for (int i = 0; i < numLayers - 1; i++) {
					this.nn.setLayerActivation(i, ActivationFunction.RELU_ACTIVATION);
				}
				this.nn.setLayerActivation(this.nn.getLayerSizes().length - 1, ActivationFunction.LINEAR_ACTIVATION);
				scan.close();
			} catch (FileNotFoundException e) {
				System.err.print("Neural net load file not found!");
				System.err.println(" Please check the file name in setting neuralnet.loadfile");
				e.printStackTrace();
			}
		}

	}


	private void takeAction(int actionNum) {
		if (actionNum == 0) {
			this.actuator.moveRight();
		} else if (actionNum == 1) {
			this.actuator.moveUp();
		} else if (actionNum == 2) {
			this.actuator.moveLeft();
		} else if (actionNum == 3) {
			this.actuator.moveDown();
		} else {
			this.actuator.coverCurrentNode();
		}
	}


	@Override
	public void reloadSettings() {
		this.actuator.reloadSettings();
		this.sensor.reloadSettings();
		this.preprocessor.reloadSettings();

		this.DISCOUNT_FACTOR = AdversarialCoverage.settings.getDoubleProperty("deepql.discountfactor");
		this.GREEDY_EPSILON_DECREMENT = AdversarialCoverage.settings.getDoubleProperty("deepql.greedy_epsilon_decrement");
		this.GREEDY_EPSILON_MINIMUM = AdversarialCoverage.settings.getDoubleProperty("deepql.greedy_epsilon_minimum");
		this.MINIBATCH_SIZE = AdversarialCoverage.settings.getIntProperty("deepql.minibatch_size");
		this.LEARNING_RATE_DECAY_FACTOR = AdversarialCoverage.settings.getDoubleProperty("deepql.learning_rate_decay_factor");

		if (AdversarialCoverage.settings.getStringProperty("neuralnet.trainingtype").equalsIgnoreCase("rmsprop")) {
			this.NN_TRAINING_TYPE = TrainingType.RMSPROP;
		} else {
			this.NN_TRAINING_TYPE = TrainingType.MOMENTUM;
		}

		this.PRINT_Q_VALUES = AdversarialCoverage.settings.getBooleanProperty("deepql.display.print_q_values");
		this.HIDDEN_LAYER_SIZE = AdversarialCoverage.settings.getIntProperty("neuralnet.hidden_layer_size");
		this.HISTORY_MAX = AdversarialCoverage.settings.getIntProperty("deepql.history_max");
		this.NUM_HIDDEN_LAYERS = AdversarialCoverage.settings.getIntProperty("neuralnet.num_hidden_layers");
		this.USING_EXTERNAL_QLEARNER = AdversarialCoverage.settings.getStringProperty("deepql.nn_setup_mode").equalsIgnoreCase("torch")
				&& AdversarialCoverage.settings.getBooleanProperty("deepql.use_external_qlearner");
	}

	public class StateTransition {
		public boolean isTerminal;
		public double[] nnInput;
		// double[] resultState;
		public double reward;
		double correctQVal;
		public int action;
		public double[] nextInput;


		StateTransition(double[] state) {
			this.nnInput = state;
		}
	}
}

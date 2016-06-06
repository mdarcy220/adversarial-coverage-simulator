package coveragealgorithms;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Random;
import java.util.Scanner;

import adversarialcoverage.AdversarialCoverage;
import adversarialcoverage.GridActuator;
import adversarialcoverage.GridSensor;
import deeplearning.NeuralNet;
import deeplearning.StatePreprocessor;
import deeplearning.StateTransition;
import deeplearning.NeuralNet.TrainingType;
import deeplearning.ActivationFunction;
import deeplearning.ExternalTorchNN;

/**
 * Deep Q-Learning based grid coverage, using a custom Java-based neural network
 * implementation.
 * 
 * @author Mike D'Arcy
 *
 */
public class DQLGC implements GridCoverageAlgorithm {
	private NeuralNet nn = null;
	private GridSensor sensor;
	private GridActuator actuator;
	private Random randgen = new Random();
	private StatePreprocessor preprocessor;
	private double greedyEpsilon = AdversarialCoverage.settings.getDoubleProperty("deepql.greedy_epsilon_start");
	private long stepNum = 0;
	private StateTransition[] lastStates;
	private int VISION_RADIUS = AdversarialCoverage.settings.getIntProperty("deepql.nn_input.vision_radius");
	private final int NN_INPUT_SIZE = calcStateSize();
	private int HISTORY_MAX = AdversarialCoverage.settings.getIntProperty("deepql.history_max");
	private double DISCOUNT_FACTOR = AdversarialCoverage.settings.getDoubleProperty("deepql.discountfactor");
	private double GREEDY_EPSILON_DECREMENT = AdversarialCoverage.settings.getDoubleProperty("deepql.greedy_epsilon_decrement");
	private double GREEDY_EPSILON_MINIMUM = AdversarialCoverage.settings.getDoubleProperty("deepql.greedy_epsilon_minimum");
	private int MINIBATCH_SIZE = AdversarialCoverage.settings.getIntProperty("deepql.minibatch_size");
	private double LEARNING_RATE_DECAY_FACTOR = AdversarialCoverage.settings.getDoubleProperty("deepql.learning_rate_decay_factor");
	private TrainingType NN_TRAINING_TYPE = TrainingType.RMSPROP;
	private boolean PRINT_Q_VALUES = AdversarialCoverage.settings.getBooleanProperty("deepql.display.print_q_values");
	private int HIDDEN_LAYER_SIZE = AdversarialCoverage.settings.getIntProperty("neuralnet.hidden_layer_size");
	private int NUM_HIDDEN_LAYERS = AdversarialCoverage.settings.getIntProperty("neuralnet.num_hidden_layers");
	private boolean USING_EXTERNAL_QLEARNER = AdversarialCoverage.settings.getStringProperty("deepql.nn_setup_mode").equalsIgnoreCase("torch")
			&& AdversarialCoverage.settings.getBooleanProperty("deepql.use_external_qlearner");
	private boolean EXTERNALNN_ALLOW_PARTIAL_TRANSITIONS = AdversarialCoverage.settings.getBooleanProperty("neuralnet.torch.use_partial_transitions");
	private int LOSS_SAMPLING_RATE = AdversarialCoverage.settings.getIntProperty("logging.deepql.loss_sampling_interval");
	
	private long stateHistorySize = 0;


	private int calcStateSize() {
		int layersize = this.VISION_RADIUS * this.VISION_RADIUS;
		int numLayers = 3 + (AdversarialCoverage.settings.getBooleanProperty("deepql.nn_input.obstacle_layer") ? 1 : 0);
		int miscInputs = (AdversarialCoverage.settings.getBooleanProperty("neuralnet.give_global_pos_and_size") ? 4 : 0);

		return (layersize * numLayers) + miscInputs;
	}


	public DQLGC(GridSensor sensor, GridActuator actuator) {
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
		}
	}


	@Override
	public void step() {
		double[] curState = this.preprocessor.getPreprocessedState();
		double[] nnInput = new double[this.NN_INPUT_SIZE];

		for (int i = 0; i < this.NN_INPUT_SIZE; i++) {
			nnInput[i] = curState[i];
		}

		StateTransition transition = new StateTransition();
		transition.nnInput = nnInput;

		int action = -1;
		double[] nnOutput = null;

		if (this.randgen.nextDouble() < this.greedyEpsilon) {
			action = this.randgen.nextInt(5);
		} else {
			this.nn.feedForward(nnInput);
			nnOutput = this.nn.getOutputs();
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

			action = maxIndex;
		}
		transition.action = action;
		this.actuator.takeActionById(action);
		curState = this.preprocessor.getPreprocessedState();
		transition.nextInput = curState;

		transition.reward = this.actuator.getLastReward();
		transition.correctQVal = transition.reward;
		transition.isTerminal = this.sensor.isFinished();

		if (this.stepNum % this.LOSS_SAMPLING_RATE == 0) {
			showLossForTransition(transition, nnOutput);
		}

		this.trainMinibatch(transition);

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


	private void showLossForTransition(StateTransition transition, double[] tmpNNOutput) {
		double[] nnOutput = tmpNNOutput;
		if (nnOutput == null) {
			this.nn.feedForward(transition.nnInput);
			nnOutput = this.nn.getOutputs();
		}

		if (!this.sensor.isFinished()) {
			this.nn.feedForward(transition.nextInput);
			double[] nnOutput2 = this.nn.getOutputs();
			double maxVal = fastMax_DoubleArr5(nnOutput2);

			transition.correctQVal += this.DISCOUNT_FACTOR * maxVal;
		}
		if (this.PRINT_Q_VALUES) {
			System.out.printf("\"correct\" q-value for %d: %f\n", transition.action, transition.correctQVal);
		}


		System.out.printf("Loss for minibatch %d: %f\n", this.stepNum, transition.correctQVal - nnOutput[transition.action]);
	}


	private void trainMinibatch(StateTransition transition) {
		if (!this.USING_EXTERNAL_QLEARNER) {
			this.lastStates[(int) (this.stepNum % this.HISTORY_MAX)] = transition;
			this.stateHistorySize = Math.min(this.HISTORY_MAX, this.stepNum);
		}

		if (this.USING_EXTERNAL_QLEARNER && (this.nn instanceof ExternalTorchNN)) {
			((ExternalTorchNN) this.nn).sendTransition(transition, this.EXTERNALNN_ALLOW_PARTIAL_TRANSITIONS);
			((ExternalTorchNN) this.nn).runTorchMinibatch();
		} else if (this.MINIBATCH_SIZE < this.stepNum) {
			trainMinibatchFromHistory(this.MINIBATCH_SIZE);
		}
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
			this.nn = new ExternalTorchNN(
					prefix + AdversarialCoverage.settings.getStringProperty("deepql.external_torch_nn.nninput_file_name"),
					prefix + AdversarialCoverage.settings.getStringProperty("deepql.external_torch_nn.nnoutput_file_name"));
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
		this.LOSS_SAMPLING_RATE = AdversarialCoverage.settings.getIntProperty("logging.deepql.loss_sampling_interval");
		this.EXTERNALNN_ALLOW_PARTIAL_TRANSITIONS = AdversarialCoverage.settings.getBooleanProperty("neuralnet.torch.use_partial_transitions");
	}
}

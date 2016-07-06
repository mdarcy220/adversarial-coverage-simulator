package coveragealgorithms;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.Random;
import java.util.Scanner;

import adversarialcoverage.AdversarialCoverage;
import adversarialcoverage.AdversarialCoverageSettings;
import adversarialcoverage.GridActuator;
import adversarialcoverage.GridSensor;
import adversarialcoverage.TerminalCommand;
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
	private double greedyEpsilon = AdversarialCoverage.settings.getDouble("deepql.greedy_epsilon_start");
	private long stepNum = 0;
	private StateTransition[] lastStates;
	private int HISTORY_MAX;
	private double DISCOUNT_FACTOR;
	private double GREEDY_EPSILON_DECREMENT;
	private double GREEDY_EPSILON_MINIMUM;
	private int MINIBATCH_SIZE;
	private double LEARNING_RATE_DECAY_FACTOR;
	private TrainingType NN_TRAINING_TYPE = TrainingType.RMSPROP;
	private boolean PRINT_Q_VALUES;
	private int HIDDEN_LAYER_SIZE;
	private int NUM_HIDDEN_LAYERS;
	private boolean USING_EXTERNAL_QLEARNER;
	private boolean EXTERNALNN_ALLOW_PARTIAL_TRANSITIONS;
	private boolean ALWAYS_FORWARD_NNINPUT;
	private int LOSS_SAMPLING_RATE;
	private int MINIBATCH_INTERVAL;
	private int EXTERNAL_RNN_NUM_CODES_PER_MINIBATCH;
	private MinibatchSeqType MINIBATCH_SEQ_TYPE = MinibatchSeqType.MANUAL;

	private long stateHistorySize = 0;
	private double[] nnOutput = null;
	private long lastTerminalStep = -1;


	public DQLGC(GridSensor sensor, GridActuator actuator) {
		this.sensor = sensor;
		this.actuator = actuator;
		this.preprocessor = new StatePreprocessor(this.sensor);

		this.reloadSettings();

		this.lastStates = new StateTransition[this.HISTORY_MAX];

		this.registerCustomCommands();
	}


	@Override
	public void init() {
		if (this.nn == null) {
			this.reloadSettings();
			this.initNeuralNet();
		}
	}


	@Override
	public void step() {
		if (this.stepNum % 25000 == 0) {
			this.nn.LEARNING_RATE *= this.LEARNING_RATE_DECAY_FACTOR;
			System.out.println(this.nn.exportToString());
			System.out.println("Minibatch number=" + this.stepNum);
			System.out.println("Epsilon=" + this.greedyEpsilon);
			System.out.println("Learning rate=" + this.nn.LEARNING_RATE);
		}

		double[] curState = this.preprocessor.getPreprocessedState();
		double[] nnInput = new double[this.preprocessor.getNNInputSize()];

		for (int i = 0; i < this.preprocessor.getNNInputSize(); i++) {
			nnInput[i] = curState[i];
		}

		StateTransition transition = new StateTransition();
		transition.nnInput = nnInput;

		if (this.nnOutput == null && this.ALWAYS_FORWARD_NNINPUT) {
			this.nnOutput = ensureNNOutput(nnInput);
		}

		decideTransitionAction(transition, nnInput);
		this.actuator.takeActionById(transition.action);

		transition.reward = this.actuator.getLastReward();
		transition.nextInput = this.preprocessor.getPreprocessedState();
		transition.correctQVal = transition.reward;
		transition.isTerminal = this.sensor.isFinished();

		if ((this.LOSS_SAMPLING_RATE != 0) && (this.stepNum % this.LOSS_SAMPLING_RATE == 0)) {
			showLoss(transition);
		} else {
			this.nnOutput = null;
		}

		this.storeTranstion(transition);

		if (this.MINIBATCH_SEQ_TYPE == MinibatchSeqType.MANUAL) {
			if (this.stepNum % this.MINIBATCH_INTERVAL == 0) {
				this.trainMinibatch();
			}
		} else if (this.MINIBATCH_SEQ_TYPE == MinibatchSeqType.FULL_EPISODE) {
			if (transition.isTerminal) {
				this.trainMinibatch();
			}
		}

		if (transition.isTerminal) {
			this.lastTerminalStep = this.stepNum;
			this.nn.forget();
		}

		this.updateGreedyEpsilon();

		this.stepNum++;
	}


	private double[] ensureNNOutput(double[] nnInput) {
		this.nn.feedForward(nnInput);
		double[] tmpOutputs = this.nn.getOutputs();

		if (tmpOutputs == null) {
			System.err.println("Null nn output. Sleeping for 10 seconds.");
			try {
				Thread.sleep(10000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			tmpOutputs = new double[5];
		}
		return tmpOutputs;
	}


	private void updateGreedyEpsilon() {
		if (this.GREEDY_EPSILON_MINIMUM < this.greedyEpsilon) {
			this.greedyEpsilon -= this.GREEDY_EPSILON_DECREMENT;
		} else {
			this.greedyEpsilon = this.GREEDY_EPSILON_MINIMUM;
		}
	}


	private void decideTransitionAction(StateTransition transition, double[] nnInput) {
		if (this.randgen.nextDouble() < this.greedyEpsilon) {
			transition.action = this.randgen.nextInt(5);
		} else {
			if (this.nnOutput == null) {
				this.nnOutput = ensureNNOutput(nnInput);
			}
			if (this.PRINT_Q_VALUES) {
				printQVals(this.nnOutput);
			}
			double maxVal = Double.NEGATIVE_INFINITY;
			int maxIndex = 0;
			for (int i = 0; i < this.nnOutput.length; i++) {
				if (maxVal < this.nnOutput[i]) {
					maxVal = this.nnOutput[i];
					maxIndex = i;
				}
			}

			transition.action = maxIndex;
		}
	}


	private void storeTranstion(StateTransition transition) {
		if (!this.USING_EXTERNAL_QLEARNER) {
			this.lastStates[(int) (this.stepNum % this.HISTORY_MAX)] = transition;
			this.stateHistorySize = Math.min(this.HISTORY_MAX, this.stepNum);
		} else if (this.nn instanceof ExternalTorchNN) {
			((ExternalTorchNN) this.nn).sendTransition(transition, this.EXTERNALNN_ALLOW_PARTIAL_TRANSITIONS);
		}
	}


	private void showLoss(StateTransition transition) {
		if (this.nnOutput == null) {
			this.nnOutput = ensureNNOutput(transition.nnInput);
		}

		double[] initialOutputs = Arrays.copyOf(this.nnOutput, this.nnOutput.length);

		if (!transition.isTerminal) {
			this.nnOutput = ensureNNOutput(transition.nextInput);
			double maxVal = fastMax_DoubleArr5(this.nnOutput);

			transition.correctQVal += this.DISCOUNT_FACTOR * maxVal;
		}
		if (this.PRINT_Q_VALUES) {
			System.out.printf("\"correct\" q-value for %d: %f\n", transition.action, transition.correctQVal);
		}

		double linearLoss = transition.correctQVal - initialOutputs[transition.action];
		// double squaredLoss = linearLoss*linearLoss;

		System.out.printf("Loss for minibatch %d: %f\n", this.stepNum, linearLoss);
	}


	private void trainMinibatch() {
		if (this.USING_EXTERNAL_QLEARNER && (this.nn instanceof ExternalTorchNN)) {
			long numCodesToSend = 1;
			if (this.MINIBATCH_SEQ_TYPE == MinibatchSeqType.MANUAL) {
				numCodesToSend = this.EXTERNAL_RNN_NUM_CODES_PER_MINIBATCH;
			} else if (this.MINIBATCH_SEQ_TYPE == MinibatchSeqType.FULL_EPISODE) {
				int nCodesFixed = AdversarialCoverage.settings.getInt("deepql.minibatch_seq.fullep.numCodes");
				numCodesToSend = nCodesFixed < 0 ? (this.stepNum - this.lastTerminalStep) : nCodesFixed;
			}
			for (int i = 0; i < numCodesToSend; i++) {
				((ExternalTorchNN) this.nn).runTorchMinibatch();
			}
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
			StateTransition sample = this.lastStates[sampleNum];
			
			double[] tmpnnOutput = ensureNNOutput(sample.nextInput);
			double nextQVal = fastMax_DoubleArr5(tmpnnOutput);

			tmpnnOutput = ensureNNOutput(sample.nnInput);

			double[] correctOut = new double[5];
			for (int j = 0; j < tmpnnOutput.length; j++) {
				correctOut[j] = tmpnnOutput[j];
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
		final String setupMode = AdversarialCoverage.settings.getString("deepql.nn_setup_mode");
		if (setupMode.equalsIgnoreCase("native")) {
			this.nn = new NeuralNet(new int[] { this.preprocessor.getNNInputSize(), 1 });
			this.nn.removeLastLayer();
			for (int i = 0; i < this.NUM_HIDDEN_LAYERS; i++) {
				this.nn.addFullyConnectedLayer(this.HIDDEN_LAYER_SIZE, ActivationFunction.RELU_ACTIVATION);
			}
			this.nn.addFullyConnectedLayer(5, ActivationFunction.LINEAR_ACTIVATION);
			final int[] nnLayerSizes = this.nn.getLayerSizes();
			this.nn.removeNeuronFromLayer(nnLayerSizes.length - 1, nnLayerSizes[nnLayerSizes.length - 1] - 1);
			this.nn.trainingType = this.NN_TRAINING_TYPE;
		} else if (setupMode.equalsIgnoreCase("torch")) {
			String prefix = AdversarialCoverage.settings.getString("deepql.external_torch_nn.io_file_prefix");
			this.nn = new ExternalTorchNN(prefix + AdversarialCoverage.settings.getString("deepql.external_torch_nn.nninput_file_name"),
					prefix + AdversarialCoverage.settings.getString("deepql.external_torch_nn.nnoutput_file_name"));
			System.out.println("Using Torch neural network...");
		} else {

			try {
				Scanner scan = new Scanner(new File(AdversarialCoverage.settings.getString("neuralnet.loadfile")));
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


	/**
	 * Registers commands for this class to the main console controller.
	 */
	private void registerCustomCommands() {
		AdversarialCoverage.controller.registerCommand(":dqlgc_get", new TerminalCommand() {
			@Override
			public void execute(String[] args) {
				if (args.length < 1) {
					return;
				}
				printDQLGCProperty(args[0]);
			}
		});
	}


	private void printDQLGCProperty(String propertyName) {
		if (propertyName.equals("greedy_epsilon")) {
			System.out.printf("%f", this.greedyEpsilon);
		} else if (propertyName.equals("stepNum")) {
			System.out.printf("%d", this.stepNum);
		}
	}


	@Override
	public void reloadSettings() {
		this.actuator.reloadSettings();
		this.sensor.reloadSettings();
		this.preprocessor.reloadSettings();

		final AdversarialCoverageSettings settings = AdversarialCoverage.settings;

		this.DISCOUNT_FACTOR = settings.getDouble("deepql.discountfactor");
		this.GREEDY_EPSILON_DECREMENT = settings.getDouble("deepql.greedy_epsilon_decrement");
		this.GREEDY_EPSILON_MINIMUM = settings.getDouble("deepql.greedy_epsilon_minimum");
		this.MINIBATCH_SIZE = settings.getInt("deepql.minibatch_size");
		this.LEARNING_RATE_DECAY_FACTOR = settings.getDouble("deepql.learning_rate_decay_factor");

		if (settings.getString("neuralnet.trainingtype").equalsIgnoreCase("rmsprop")) {
			this.NN_TRAINING_TYPE = TrainingType.RMSPROP;
		} else {
			this.NN_TRAINING_TYPE = TrainingType.MOMENTUM;
		}

		this.PRINT_Q_VALUES = settings.getBoolean("deepql.display.print_q_values");
		this.HIDDEN_LAYER_SIZE = settings.getInt("neuralnet.hidden_layer_size");
		this.HISTORY_MAX = settings.getInt("deepql.history_max");
		this.NUM_HIDDEN_LAYERS = settings.getInt("neuralnet.num_hidden_layers");
		this.USING_EXTERNAL_QLEARNER = settings.getString("deepql.nn_setup_mode").equalsIgnoreCase("torch")
				&& settings.getBoolean("deepql.use_external_qlearner");
		this.LOSS_SAMPLING_RATE = settings.getInt("logging.deepql.loss_sampling_interval");
		this.EXTERNALNN_ALLOW_PARTIAL_TRANSITIONS = settings.getBoolean("neuralnet.torch.use_partial_transitions");
		this.ALWAYS_FORWARD_NNINPUT = settings.getBoolean("deepql.always_forward_nninput");
		this.MINIBATCH_INTERVAL = settings.getInt("deepql.minibatch_interval");
		this.EXTERNAL_RNN_NUM_CODES_PER_MINIBATCH = settings.getInt("deepql.external.rnn.num_codes_per_minibatch");
		this.MINIBATCH_SEQ_TYPE = MinibatchSeqType.fromString(settings.getString("deepql.minibatch_seq_type"));
	}


	enum MinibatchSeqType {
		MANUAL, FULL_EPISODE;

		static MinibatchSeqType fromString(String str) {
			if (str.equals("manual")) {
				return MANUAL;
			} else if (str.equals("full_episode")) {
				return FULL_EPISODE;
			}

			return MANUAL;
		}
	}
}

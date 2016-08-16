package simulations.generic.algo;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.Random;
import java.util.Scanner;

import adsim.Algorithm;
import adsim.SimulatorMain;
import adsim.SimulatorSettings;
import adsim.TerminalCommand;
import adsim.stats.SampledVariableDouble;
import deeplearning.NeuralNet;
import deeplearning.StatePreprocessor;
import deeplearning.StateTransition;
import deeplearning.NeuralNet.TrainingType;
import deeplearning.DQLActuator;
import gridenv.GridSensor;
import simulations.coverage.CoverageGridActuator;
import deeplearning.ActivationFunction;
import deeplearning.ExternalTorchNN;

/**
 * Deep Q-Learning based generic problem-solving algorithm. The main DQL training can
 * either be done by this class or by an external DQL engine, and the NN backend can set
 * to either a Java-based implementation or an external one.
 * 
 * @author Mike D'Arcy
 *
 */
public class DQL implements Algorithm {
	private boolean PRINT_Q_VALUES;
	private boolean USING_EXTERNAL_QLEARNER;
	private boolean EXTERNALNN_ALLOW_PARTIAL_TRANSITIONS;
	private boolean EXTERNALNN_USE_FAST_FORWARDS;
	private boolean ALWAYS_FORWARD_NNINPUT;
	private double greedyEpsilon = SimulatorMain.settings.getDouble("deepql.greedy_epsilon_start");
	private double DISCOUNT_FACTOR;
	private double GREEDY_EPSILON_DECREMENT;
	private double GREEDY_EPSILON_MINIMUM;
	private double LEARNING_RATE_DECAY_FACTOR;
	private int EXTERNAL_RNN_NUM_CODES_PER_MINIBATCH;
	private int HIDDEN_LAYER_SIZE;
	private int HISTORY_MAX;
	private int LOSS_SAMPLING_INTERVAL;
	private int LOSS_DISPLAY_INTERVAL;
	private int MINIBATCH_INTERVAL;
	private int MINIBATCH_SIZE;
	private int NUM_HIDDEN_LAYERS;
	private long lastTerminalStep = -1;
	private long stateHistorySize = 0;
	private long stepNum = 0;
	private DQLActuator actuator;
	private GridSensor sensor;
	private MinibatchSeqType MINIBATCH_SEQ_TYPE = MinibatchSeqType.MANUAL;
	private NeuralNet nn = null;
	private Random randgen = new Random();
	private SampledVariableDouble trainingLoss = new SampledVariableDouble();
	private SampledVariableDouble trainingAbsLoss = new SampledVariableDouble();
	private StatePreprocessor preprocessor;
	private StateTransition[] lastStates;
	private TrainingType NN_TRAINING_TYPE = TrainingType.RMSPROP;

	private double[] nnOutput = null;


	public DQL(GridSensor sensor, CoverageGridActuator actuator) {
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

		double[] nnInput = this.preprocessor.getPreprocessedState();

		StateTransition transition = new StateTransition();
		transition.nnInput = nnInput;

		if (this.nnOutput == null && this.ALWAYS_FORWARD_NNINPUT) {
			this.nnOutput = ensureNNOutput(nnInput);
		}

		decideTransitionAction(transition, nnInput);
		this.actuator.takeActionById(transition.action);

		transition.reward = this.actuator.getLastReward();
		transition.nextInput = this.preprocessor.getPreprocessedState();
		transition.isTerminal = this.sensor.isFinished();

		if ((this.LOSS_SAMPLING_INTERVAL != 0) && (this.stepNum % this.LOSS_SAMPLING_INTERVAL == 0)) {
			double loss = calcLoss(transition);
			this.trainingLoss.addSample(loss);
			this.trainingAbsLoss.addSample(Math.abs(loss));
		} else {
			this.nnOutput = null;
		}

		if ((this.LOSS_DISPLAY_INTERVAL != 0) && (this.stepNum % this.LOSS_DISPLAY_INTERVAL == 0)) {
			double loss_extreme = (Math.abs(this.trainingLoss.getMax()) < Math.abs(this.trainingLoss.getMin()))
					? this.trainingLoss.getMin() : this.trainingLoss.getMax();
			System.out.printf("Loss stats: n=%d, mx=%.4f, avg=%.5f (%.5f), avgmag=%.6f\n", this.trainingLoss.numSamples(), loss_extreme,
					this.trainingLoss.mean(), this.trainingLoss.stddev(), this.trainingAbsLoss.mean(),
					this.trainingAbsLoss.stddev());
			this.trainingLoss.reset();
			this.trainingAbsLoss.reset();
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


	/**
	 * Feeds the given input through the network and returns the output. This method
	 * guarantees a valid output, even if the network's real output is null. If the
	 * network has null output, an array of zeros will be returned.
	 * 
	 * @param nnInput
	 *                the input to the network
	 * @return
	 */
	private double[] ensureNNOutput(double[] nnInput) {
		boolean use_fast_forward = this.EXTERNALNN_USE_FAST_FORWARDS && (this.nn instanceof ExternalTorchNN)
				&& (this.lastTerminalStep < (this.stepNum - 2)) && 0 < this.stepNum;
		if (use_fast_forward) {
			((ExternalTorchNN) this.nn).feedForward_noSendState();
		} else {
			this.nn.feedForward(nnInput);
		}
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


	private double calcLoss(StateTransition transition) {
		if (this.nnOutput == null) {
			this.nnOutput = ensureNNOutput(transition.nnInput);
		}

		double[] initialOutputs = Arrays.copyOf(this.nnOutput, this.nnOutput.length);
		double correctQVal = transition.reward;

		if (!transition.isTerminal) {
			this.nnOutput = ensureNNOutput(transition.nextInput);
			double maxVal = fastMax_DoubleArr5(this.nnOutput);

			correctQVal += this.DISCOUNT_FACTOR * maxVal;
		}
		if (this.PRINT_Q_VALUES) {
			System.out.printf("\"correct\" q-value for %d: %f\n", transition.action, correctQVal);
		}

		double linearLoss = correctQVal - initialOutputs[transition.action];

		return linearLoss;
	}


	private void trainMinibatch() {
		if (this.USING_EXTERNAL_QLEARNER && (this.nn instanceof ExternalTorchNN)) {
			long numCodesToSend = 1;
			if (this.MINIBATCH_SEQ_TYPE == MinibatchSeqType.MANUAL) {
				numCodesToSend = this.EXTERNAL_RNN_NUM_CODES_PER_MINIBATCH;
			} else if (this.MINIBATCH_SEQ_TYPE == MinibatchSeqType.FULL_EPISODE) {
				int nCodesFixed = SimulatorMain.settings.getInt("deepql.minibatch_seq.fullep.numCodes");
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
		final String setupMode = SimulatorMain.settings.getString("deepql.nn_setup_mode");
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
			String prefix = SimulatorMain.settings.getString("deepql.external_torch_nn.io_file_prefix");
			this.nn = new ExternalTorchNN(prefix + SimulatorMain.settings.getString("deepql.external_torch_nn.nninput_file_name"),
					prefix + SimulatorMain.settings.getString("deepql.external_torch_nn.nnoutput_file_name"));
			System.out.println("Using Torch neural network...");
		} else {

			try {
				Scanner scan = new Scanner(new File(SimulatorMain.settings.getString("neuralnet.loadfile")));
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
		SimulatorMain.controller.registerCommand(":dql_get", new TerminalCommand() {
			@Override
			public void execute(String[] args) {
				if (args.length < 1) {
					return;
				}
				printDQLProperty(args[0]);
			}
		});
	}


	private void printDQLProperty(String propertyName) {
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

		final SimulatorSettings settings = SimulatorMain.settings;

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
		this.LOSS_SAMPLING_INTERVAL = settings.getInt("logging.deepql.loss_sampling_interval");
		this.LOSS_DISPLAY_INTERVAL = settings.getInt("logging.deepql.loss_display_interval");
		this.EXTERNALNN_ALLOW_PARTIAL_TRANSITIONS = settings.getBoolean("neuralnet.torch.use_partial_transitions");
		this.EXTERNALNN_USE_FAST_FORWARDS = settings.getBoolean("deepql.external.use_fast_forwards");
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

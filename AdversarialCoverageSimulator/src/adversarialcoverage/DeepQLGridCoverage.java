package adversarialcoverage;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

import deeplearning.NeuralNet;
import deeplearning.ActivationFunction;

public class DeepQLGridCoverage extends CoverageAlgorithm {
	NeuralNet nn = null;
	GridSensor sensor;
	GridActuator actuator;
	Random randgen = new Random();
	double greedyEpsilon = 1;
	long stepNum = 0;
	List<StateTransition> lastStates = new ArrayList<>();
	final static int STATE_SIZE = 75;
	final static int HISTORY_MAX = 100000;
	final static double DISCOUNT_FACTOR = 0.5;
	final static double GREEDY_EPSILON_DECREMENT = 0.0000005;
	final static int MINIBATCH_SIZE = 32;


	private double[] getPreprocessedState() {

		double[] curState = new double[STATE_SIZE];
		for (int x = 0; x < 5; x++) {
			for (int y = 0; y < 5; y++) {
				curState[x * 5 + y] = this.sensor.getDangerLevelAt(x, y);
				curState[25 + x * 5 + y] = this.sensor.getCoverCountAt(x, y) < 1 ? -1 : 1;
				curState[50 + x * 5 + y] = 0;
			}
		}
		curState[50 + this.sensor.getLocation().x * 5 + this.sensor.getLocation().y] = 1;

		// Normalize the data
		// The sum of squares should be around 27 (1 for danger levels +
		// 25 for coverage + 1 for location)
		double approxStdDev = Math.sqrt(27.0);
		for (int i = 0; i < STATE_SIZE; i++) {
			curState[i] /= approxStdDev;
		}

		return curState;
	}


	public DeepQLGridCoverage(GridSensor sensor, GridActuator actuator) {
		this.sensor = sensor;
		this.actuator = actuator;
	}


	@Override
	public void init() {
		if (this.nn == null) {
			double[] state = this.getPreprocessedState();
			StateTransition trans = new StateTransition(state);
			trans.nextInput = state;
			this.lastStates.add(trans);

			initNeuralNet();
			printNeuralNetParams();
		}
	}


	private void printNeuralNetParams() {
		System.out.printf("LEARNING_RATE=%f\n", this.nn.LEARNING_RATE);
		System.out.printf("MOMENTUM_FACTOR=%f\n", this.nn.MOMENTUM_GAMMA);
		System.out.printf("GREEDINESS=%f\n", (1.0 - this.greedyEpsilon));
		System.out.printf("GREEDINESS_INCREMENT=%f\n", (1.0 - DeepQLGridCoverage.GREEDY_EPSILON_DECREMENT));
		System.out.printf("MINIBATCH_SIZE=%d\n", DeepQLGridCoverage.MINIBATCH_SIZE);
		System.out.printf("DISCOUNT_FACTOR=%f\n", DeepQLGridCoverage.DISCOUNT_FACTOR);
		System.out.printf("STATE_SIZE=%d\n", DeepQLGridCoverage.STATE_SIZE);
		System.out.printf("HISTORY_MAX=%d\n", DeepQLGridCoverage.HISTORY_MAX);
		System.out.printf("ROBOTS_BREAKABLE=%b\n",
				AdversarialCoverage.settings.getBooleanProperty("robots.breakable"));
		System.out.printf("GRID_GENERATOR_STRING=%s\n",
				AdversarialCoverage.settings.getStringProperty("env.grid.dangervalues"));

		System.out.printf("NEURAL_NET_LAYERS=[");
		int[] layerSizes = this.nn.getLayerSizes();
		for (int i = 0; i < layerSizes.length - 1; i++) {
			System.out.printf("%d, ", layerSizes[i]);
		}
		System.out.printf("%d", layerSizes[layerSizes.length - 1]);
		System.out.printf("]\n");
	}


	@Override
	public void step() {
		double[] curState = getPreprocessedState();
		double[] nnInput = new double[STATE_SIZE];
		for (int i = 0; i < STATE_SIZE; i++) {
			nnInput[i] = curState[i];
		}

		StateTransition transition = new StateTransition(nnInput);
		this.nn.feedForward(nnInput);
		double[] nnOutput = this.nn.getOutputs();
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
		curState = getPreprocessedState();

		double[] nnInput2 = new double[STATE_SIZE];
		for (int i = 0; i < STATE_SIZE; i++) {
			nnInput2[i] = curState[i];
		}
		transition.nextInput = curState;

		transition.reward = this.actuator.getLastReward();
		transition.correctQVal = transition.reward;
		transition.isTerminal = true;
		if (!this.sensor.env.isFinished()) {
			this.nn.feedForward(curState);
			double[] nnOutput2 = this.nn.getOutputs();
			maxVal = Double.NEGATIVE_INFINITY;
			for (int i = 0; i < nnOutput2.length; i++) {
				if (maxVal < nnOutput2[i]) {
					maxVal = nnOutput2[i];
				}
			}

			transition.correctQVal += DeepQLGridCoverage.DISCOUNT_FACTOR * maxVal;
			transition.isTerminal = false;
		}
		transition.correctQVal /= 4.0;

		double[] correctOut = new double[5];
		for (int i = 0; i < nnOutput.length; i++) {
			correctOut[i] = nnOutput[i];
		}
		correctOut[action] = transition.correctQVal;
		if (this.stepNum % 500 == 0) {
			double squaredErrorSum = this.getSquaredErrorSum(correctOut[action], nnOutput[action]);
			System.out.printf("Loss for minibatch %d: %f\n", this.stepNum,
					correctOut[action] - nnOutput[action]);
			if (squaredErrorSum < 0.00002) {
				this.nn.LEARNING_RATE *= 0.99;
			}
		}
		this.lastStates.add(transition);
		if (HISTORY_MAX < this.lastStates.size()) {
			this.lastStates.remove(0);
		}

		trainMinibatchFromHistory(32);

		if (this.stepNum % 50000 == 0) {
			System.out.println(this.nn.exportToString());
			System.out.println("Minibatch number=" + this.stepNum);
			System.out.println("Epsilon=" + this.greedyEpsilon);
		}
		if (0.1 < this.greedyEpsilon) {
			this.greedyEpsilon -= 0.0000005;
		} else {
			this.greedyEpsilon = 0.1;
		}


		this.stepNum++;
	}


	private void trainMinibatchFromHistory(int batchSize) {
		for (int i = 0; i < batchSize; i++) {
			int sampleNum = this.randgen.nextInt(this.lastStates.size());
			StateTransition sample = this.lastStates.get(sampleNum);
			this.nn.feedForward(sample.nextInput);
			double nextQVal = this.nn.getOutputs()[sample.action];
			this.nn.feedForward(sample.nnInput);
			double[] nnOutput = this.nn.getOutputs();
			double[] correctOut = new double[5];
			for (int j = 0; j < nnOutput.length; j++) {
				correctOut[j] = nnOutput[j];
			}
			correctOut[sample.action] = (sample.reward
					+ (sample.isTerminal ? 0.0 : (DeepQLGridCoverage.DISCOUNT_FACTOR * nextQVal)))
					/ 4.0;
			this.nn.backPropagateFromLastSample(correctOut);
		}
		this.nn.finishBatch();
	}


	// Gets the sum of squared errors of the most recent output of the NN
	private double getSquaredErrorSum(double correctOutput, double givenOutput) {
		return (correctOutput - givenOutput) * (correctOutput - givenOutput);
	}


	private void initNeuralNet() {
		if (AdversarialCoverage.settings.getStringProperty("neuralnet.loadfile").length() == 0) {
			this.nn = new NeuralNet(new int[] { STATE_SIZE, 1 });
			this.nn.removeLastLayer();
			// this.nn.addConvolutionalLayer(3, 1);
			this.nn.addFullyConnectedLayer(75, ActivationFunction.RELU_ACTIVATION);
			this.nn.addFullyConnectedLayer(50, ActivationFunction.RELU_ACTIVATION);
			this.nn.addFullyConnectedLayer(25, ActivationFunction.RELU_ACTIVATION);
			this.nn.addFullyConnectedLayer(5, ActivationFunction.LINEAR_ACTIVATION);
			this.nn.removeNeuronFromLayer(4, 5);
		} else {
			Scanner scan = null;
			try {
				scan = new Scanner(new File(
						AdversarialCoverage.settings.getStringProperty("neuralnet.loadfile")));
			} catch (FileNotFoundException e) {
				System.err.print("Neural net load file not found!");
				System.err.println(" Please check the file name in setting neuralnet.loadfile");
				e.printStackTrace();
			}
			this.nn = new NeuralNet(scan.nextLine());
			scan.close();
			this.greedyEpsilon = 0.1;
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

	class StateTransition {
		public boolean isTerminal;
		double[] nnInput;
		// double[] resultState;
		double reward;
		double correctQVal;
		int action;
		double[] nextInput;


		StateTransition(double[] state) {
			this.nnInput = state;
		}
	}
}

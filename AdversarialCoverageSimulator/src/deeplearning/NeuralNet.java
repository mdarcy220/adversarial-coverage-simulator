package deeplearning;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

import adversarialcoverage.AdversarialCoverage;

public class NeuralNet {
	List<List<Neuron>> layers = new ArrayList<>();

	public double LEARNING_RATE = AdversarialCoverage.settings.getDoubleProperty("neuralnet.learning_rate");
	public double MOMENTUM_GAMMA = AdversarialCoverage.settings.getDoubleProperty("neuralnet.momentum");
	static Random randgen = new Random();
	static final ActivationFunction DEFAULT_ACTIVATION = ActivationFunction.RELU_ACTIVATION;


	public NeuralNet(int[] nNodesInLayer) {
		// Set up the input layer
		this.layers.add(new ArrayList<Neuron>());
		for (int i = 0; i < nNodesInLayer[0]; i++) {
			this.layers.get(0).add(new Neuron(this.layers.get(0).size(), DEFAULT_ACTIVATION));
		}
		// Add a bias neuron to the input layer
		this.layers.get(0).add(new Neuron(this.layers.get(0).size(), DEFAULT_ACTIVATION));
		this.layers.get(0).get(this.layers.get(0).size() - 1).setOutputValue(1.0);

		for (int i = 1; i < nNodesInLayer.length; i++) {
			this.layers.add(new ArrayList<Neuron>());
			for (int j = 0; j < nNodesInLayer[i]; j++) {
				Neuron n = new Neuron(this.layers.get(i).size(), DEFAULT_ACTIVATION);
				for (int k = 0; k < this.layers.get(i - 1).size(); k++) {
					n.addInput(this.layers.get(i - 1).get(k));
				}
				n.normalizeWeights();
				this.layers.get(i).add(n);
			}
			// Add a bias neuron to the layer, if it isn't the
			// output layer
			if (i < nNodesInLayer.length - 1) {
				this.layers.get(i).add(new Neuron(this.layers.get(i).size(), DEFAULT_ACTIVATION));
				this.layers.get(i).get(this.layers.get(i).size() - 1).setOutputValue(1.0);
			}
		}

		// Use linear activation for the output layer
		for (int i = 0; i < this.layers.get(this.layers.size() - 1).size(); i++) {
			this.layers.get(this.layers.size() - 1)
					.get(i).activeFunc = ActivationFunction.LINEAR_ACTIVATION;
		}
	}


	public NeuralNet(String propertiesStr) {
		Scanner scan = new Scanner(propertiesStr);
		// Set up the input layer
		int nInputNodes = 0;
		if (scan.hasNextInt()) {
			nInputNodes = scan.nextInt();
		}

		this.layers.add(new ArrayList<Neuron>());
		for (int i = 0; i < nInputNodes - 1; i++) {
			this.layers.get(0).add(new Neuron(this.layers.get(0).size(), DEFAULT_ACTIVATION));
		}
		// Add a bias neuron to the input layer
		this.layers.get(0).add(new Neuron(this.layers.get(0).size(), DEFAULT_ACTIVATION));
		this.layers.get(0).get(this.layers.get(0).size() - 1).setOutputValue(1.0);

		if (scan.hasNext()) {
			this.layers.add(new ArrayList<Neuron>());
		}

		int nConnections = 0;
		int nNodesInLastLayer = nInputNodes;
		int layerNum = 1;
		Neuron curNeuron = new Neuron(-1, null);
		while (scan.hasNext()) {

			String tok = scan.next().trim();
			if (nConnections == 0 && !tok.equals(";")) {
				curNeuron = new Neuron(this.layers.get(layerNum).size(), DEFAULT_ACTIVATION);
				this.layers.get(layerNum).add(curNeuron);
			}
			if (tok.equals(";")) {
				// Set the bias neuron from the last layer
				curNeuron.setOutputValue(1.0);
				// Set up the next layer
				nNodesInLastLayer = this.layers.get(this.layers.size() - 1).size();
				this.layers.add(new ArrayList<Neuron>());
				layerNum++;
				nConnections = 0;
				continue;
			} else if (tok.equals("n")) {
				nConnections++;
			} else if (tok.equals("r")) {
				curNeuron.addInput(this.layers.get(layerNum - 1).get(nConnections),
						NeuralNet.randgen.nextDouble() * 0.01);
				nConnections++;
			} else {
				curNeuron.addInput(this.layers.get(layerNum - 1).get(nConnections),
						Double.parseDouble(tok));
				nConnections++;
			}

			nConnections %= nNodesInLastLayer;
		}
		scan.close();
		// Use linear activation for the output layer
		for (int i = 0; i < this.layers.get(this.layers.size() - 1).size(); i++) {
			this.layers.get(this.layers.size() - 1)
					.get(i).activeFunc = ActivationFunction.LINEAR_ACTIVATION;
		}
	}


	public String exportToString() {
		StringBuilder sb = new StringBuilder("");
		sb.append(this.layers.get(0).size());
		for (int i = 1; i < this.layers.size(); i++) {
			for (int j = 0; j < this.layers.get(i).size(); j++) {
				int curNum = 0;
				Neuron n = new Neuron(-1, null);
				for (int k = 0; k < this.layers.get(i - 1).size(); k++) {
					if (curNum < this.layers.get(i).get(j).inputNeurons.size()) {
						n = this.layers.get(i).get(j).inputNeurons.get(curNum);
					}
					if (n.idInLayer == k) {
						sb.append(' ');
						sb.append(this.layers.get(i).get(j).inputWeights.get(curNum));
						curNum++;
					} else {
						sb.append(" n");
					}
				}
			}
			System.out.println();
			if (i < this.layers.size() - 1) {
				sb.append(" ;");
			}
		}
		return sb.toString();
	}


	public static void main_(/* String[] args */) throws FileNotFoundException {
		int[] nodesPerLayer = new int[] { 1, 1 };
		NeuralNet nn = new NeuralNet(nodesPerLayer);
		nn.removeLastLayer();
		// nn.addConvolutionalLayer(3, 1);
		// nn.addFullyConnectedLayer(75,
		// ActivationFunction.RELU_ACTIVATION);
		// nn.addFullyConnectedLayer(75,
		// ActivationFunction.RELU_ACTIVATION);
		nn.addFullyConnectedLayer(75, ActivationFunction.RELU_ACTIVATION);
		nn.addFullyConnectedLayer(1, ActivationFunction.LINEAR_ACTIVATION);
		nn.removeNeuronFromLayer(nn.layers.size() - 1, 1);
		// Scanner scan = new Scanner(new File("/tmp/nnex1.dat"));
		// NeuralNet nn = new NeuralNet(scan.nextLine());
		// scan.close();
		double[] inputs1 = new double[1];
		// inputs1[0] = 1;
		// inputs1[1] = 1;
		long t = System.nanoTime();
		Random randgen = new Random();
		for (int i = 0; i < 20000; i++) {
			byte num1 = (byte) randgen.nextInt(63);
			// byte num2 = (byte) randgen.nextInt(63);
			inputs1[0] = num1 / 64.0;
			// inputs1[1] = num2;
			// System.arraycopy(byteToDoubleBinaryArray(num1), 0,
			// inputs1, 0, 7);
			// System.arraycopy(byteToDoubleBinaryArray(num2), 0,
			// inputs1, 7, 7);
			System.out.printf("Weights: [%f, %f]\n", nn.layers.get(1).get(0).inputWeights.get(0),
					nn.layers.get(1).get(0).inputWeights.get(1));

			double[] correctOut = { 0.1 * num1 + 0.3 };// byteToDoubleBinaryArray((byte)
									// (num1
									// +
									// num2));//new
									// double[]{num1+num2};//
			// nn.learnFromExample(inputs1, correctOut);
			nn.addExampleToBatch(inputs1, correctOut);
			if (i % 10 == 0) {
				nn.finishBatch();
			}
			if (i % 1 == 0) {

				double squaredErrorSum = 0.0;
				nn.feedForward(inputs1);
				double[] out1 = nn.getOutputs();
				for (int j = 0; j < out1.length; j++) {
					squaredErrorSum += 0.5 * (correctOut[j] - out1[j]) * (correctOut[j] - out1[j]);
				}
				System.out.printf("%d\t%f\tNum=%d\n", i, squaredErrorSum, num1);
				if (squaredErrorSum < 0.002) {
					// nn.LEARNING_RATE *= 0.9;
				}
			}
		}
		System.out.println("Time: " + (System.nanoTime() - t) / 1000000000.0);
		// System.out.println(nn.exportToString());

		// byte num1 = 4;
		// byte num2 = 9;
		inputs1[0] = 5 / 64.0;
		// System.arraycopy(byteToDoubleBinaryArray(num1), 0, inputs1,
		// 0, 7);
		// System.arraycopy(byteToDoubleBinaryArray(num2), 0, inputs1,
		// 7, 7);
		nn.feedForward(inputs1);
		double[] out1 = nn.getOutputs();
		// System.out.printf("Result: %d + %d = %d\n", (int)inputs1[0],
		// (int)inputs1[1], doubleBinaryArrayToInt(out1));
		for (int i = 0; i < out1.length; i++) {
			System.out.printf("Output #%d: %f\n", i, out1[i]);
		}

	}
	
	
	public void setLayerActivation(int layerNum, ActivationFunction activation) {
		List<Neuron> layer = this.layers.get(layerNum);
		for(int i = 0; i < layer.size(); i++) {
			layer.get(i).setActivation(activation);
		}
	}


	/**
	 * Add a convolutional layer to the network
	 * 
	 * @param fieldSize
	 * @param stride
	 */
	public void addConvolutionalLayer(int fieldSize, int stride) {
		List<Neuron> lastLayer = this.layers.get(this.layers.size() - 1);
		int nNeurons = ((lastLayer.size() - fieldSize) / stride) + 1;
		Neuron lastLayerBiasNeuron = lastLayer.get(this.layers.get(this.layers.size() - 1).size() - 1);
		List<Neuron> convLayer = new ArrayList<>();

		for (int i = 0; i < nNeurons; i++) {
			Neuron n = new Neuron(i, DEFAULT_ACTIVATION);
			for (int j = 0; j < fieldSize; j++) {
				n.addInput(lastLayer.get(j + i * stride), randgen.nextGaussian() / lastLayer.size());
			}
			n.addInput(lastLayerBiasNeuron, randgen.nextGaussian() / lastLayer.size());
			convLayer.add(n);
		}
		Neuron n = new Neuron(nNeurons, DEFAULT_ACTIVATION);
		n.setOutputValue(1.0);
		convLayer.add(n);
		this.layers.add(convLayer);
	}


	/**
	 * Add a fully connected layer to the network
	 * 
	 * @param nNeurons
	 * @param activeFunc
	 */
	public void addFullyConnectedLayer(int nNeurons, ActivationFunction activeFunc) {
		List<Neuron> lastLayer = this.layers.get(this.layers.size() - 1);
		List<Neuron> newLayer = new ArrayList<>();
		int lastLayerSize = this.layers.get(this.layers.size() - 1).size();
		for (int i = 0; i < nNeurons; i++) {
			Neuron n = new Neuron(i, activeFunc);
			for (int j = 0; j < lastLayerSize; j++) {
				n.addInput(lastLayer.get(j), 0.0);
			}
			n.normalizeWeights();
			newLayer.add(n);
		}
		Neuron n = new Neuron(nNeurons, activeFunc);
		n.setOutputValue(1.0);
		newLayer.add(n);
		this.layers.add(newLayer);
	}


	/**
	 * Returns an int array, with each element representing the number of
	 * neurons in the corresponding layer of the net. the 0th element
	 * corresponds to the input layer.
	 * 
	 * @return
	 */
	public int[] getLayerSizes() {
		int[] ret = new int[this.layers.size()];
		for (int i = 0; i < this.layers.size(); i++) {
			ret[i] = this.layers.get(i).size();
		}
		return ret;
	}


	/**
	 * Remove a neuron from a layer of the network
	 * 
	 * @param layerNum
	 * @param neuronNum
	 */
	public void removeNeuronFromLayer(int layerNum, int neuronNum) {
		List<Neuron> layer = this.layers.get(layerNum);
		Neuron neuron = layer.get(neuronNum);
		if (layerNum < this.layers.size() - 1) {
			List<Neuron> nextLayer = this.layers.get(layerNum + 1);
			for (int i = 0; i < nextLayer.size(); i++) {
				for (int j = 0; j < nextLayer.get(i).inputNeurons.size(); j++) {
					if (nextLayer.get(i).inputNeurons.get(j).idInLayer == neuron.idInLayer) {
						nextLayer.get(i).inputNeurons.remove(j);
					}
				}
			}
		}

		layer.remove(neuronNum);

	}


	/**
	 * Remove the last layer from the network
	 */
	public void removeLastLayer() {
		this.layers.remove(this.layers.size() - 1);
	}


	@SuppressWarnings("unused")
	private static double[] byteToDoubleBinaryArray(byte in) {
		double[] bindata = new double[7];
		for (byte i = 6; 0 <= i; i--) {
			bindata[i] = 0 < (in & (1 << i)) ? 1 : 0;
		}
		return bindata;
	}


	@SuppressWarnings("unused")
	private static int doubleBinaryArrayToInt(double[] binary) {
		int out = 0;
		for (int i = 0; i < binary.length && i < 31; i++) {
			out |= ((binary[i] < 0.5 ? 0 : 1) << i);
		}
		return out;
	}


	public void feedForward(double[] inputs) {
		if (inputs.length != this.layers.get(0).size() - 1) {
			return;
		}

		// Initialize the input layer
		for (int i = 0; i < this.layers.get(0).size() - 1; i++) {
			this.layers.get(0).get(i).setOutputValue(inputs[i]);
		}

		// Feed forward
		for (int i = 1; i < this.layers.size(); i++) {
			for (int j = 0; j < this.layers.get(i).size(); j++) {
				this.layers.get(i).get(j).recalcOutput();
			}
		}
	}


	public void backPropagateFromLastSample(double[] correctOutputs) {
		double[] realOutputs = this.getOutputs();
		List<Neuron> outputNeurons = this.layers.get(this.layers.size() - 1);
		for (int i = 0; i < outputNeurons.size(); i++) {
			outputNeurons.get(i)
					.setErrorTerm(-(correctOutputs[i] - realOutputs[i])
							* outputNeurons.get(i).activeFunc.activationDerivative(
									outputNeurons.get(i).getWeightedSumOfInputs()));
			outputNeurons.get(i).backPropagate(this.MOMENTUM_GAMMA);
		}
		for (int i = this.layers.size() - 2; 0 < i; i--) {
			for (int j = 0; j < this.layers.get(i).size(); j++) {
				this.layers.get(i).get(j).recalcErrorTerm();
				this.layers.get(i).get(j).backPropagate(this.MOMENTUM_GAMMA);
			}
		}
		for (int i = 0; i < this.layers.size(); i++) {
			for (int j = 0; j < this.layers.get(i).size(); j++) {
				this.layers.get(i).get(j).resetBackPropagationTerms();
			}
		}
	}


	public double[] getOutputs() {
		// Make an array with the same size as the last layer of the
		// network
		double[] outputs = new double[this.layers.get(this.layers.size() - 1).size()];

		for (int i = 0; i < outputs.length; i++) {
			outputs[i] = this.layers.get(this.layers.size() - 1).get(i).getOutputValue();
		}

		return outputs;
	}


	public void addExampleToBatch(double[] inputs, double[] correctOutputs) {
		feedForward(inputs);
		backPropagateFromLastSample(correctOutputs);
	}


	public void finishBatch() {
		for (int i = 0; i < this.layers.size(); i++) {
			for (int j = 0; j < this.layers.get(i).size(); j++) {
				this.layers.get(i).get(j).applyWeightDeltas();
			}
		}
	}


	public void learnFromExample(double[] inputs, double[] correctOutputs) {

		for (int i = 0; i < this.layers.size(); i++) {
			for (int j = 0; j < this.layers.get(i).size(); j++) {
				this.layers.get(i).get(j).resetBackPropagationTerms();
			}
		}

		feedForward(inputs);
		this.backPropagateFromLastSample(correctOutputs);
	}


	/**
	 * Represents an artificial neuron
	 * 
	 * @author Mike D'Arcy
	 *
	 */
	class Neuron {
		List<Neuron> inputNeurons = new ArrayList<>();
		List<EditableDouble> inputWeights = new ArrayList<>();
		double[] deltaWeights = new double[0];
		long nSamples = 0;
		int idInLayer;
		ActivationFunction activeFunc;
		/**
		 * The weighted sum of inputs to this neuron.
		 */
		double weightedInputSum = 0.0;
		/**
		 * The output of this neuron
		 */
		double outputValue = 0.0;
		/**
		 * The error in the output (used for back-propagation)
		 */
		double errorTerm = 0.0;
		/**
		 * The weighted sum of error terms of the neurons in the next
		 * layer (used for back-propagation)
		 */
		double weightedErrorSum = 0.0;


		public Neuron(int idInLayer, ActivationFunction activeFunc) {
			this.idInLayer = idInLayer;
			this.activeFunc = activeFunc;
		}


		public void addInput(Neuron n) {
			this.addInput(n, NeuralNet.randgen.nextGaussian());
		}


		public void addInput(Neuron n, double weight) {
			this.inputNeurons.add(n);
			this.inputWeights.add(new EditableDouble(weight));
			this.deltaWeights = new double[this.inputWeights.size()];
		}


		public void normalizeWeights() {
			double inverseInputSizeSqrt = 1.0 / Math.sqrt(this.inputWeights.size());
			for (int i = 0; i < this.inputWeights.size(); i++) {
				this.inputWeights.get(i).value = inverseInputSizeSqrt
						* (2 * NeuralNet.randgen.nextDouble() - 1);
			}
		}
		
		
		public void setActivation(ActivationFunction activation) {
			this.activeFunc = activation;
		}


		public void applyWeightDeltas() {
			for (int i = 0; i < this.deltaWeights.length; i++) {
				double oldWeight = this.inputWeights.get(i).value;
				double newWeight = oldWeight - this.deltaWeights[i];
				// double newWeight = oldWeight -
				// LEARNING_RATE*this.deltaWeights[i]-0.001*oldWeight;
				this.inputWeights.get(i).value = newWeight;
				this.deltaWeights[i] = 0.0;
				if (Double.isNaN(this.inputWeights.get(i).value)) {
					this.inputWeights.get(i).value = randgen.nextDouble()
							/ this.inputWeights.size();
					System.out.println("ERROR: Weight is NaN. Aborting...");
					System.exit(1);
				}
			}
			this.nSamples = 0;
		}


		public void applyWeightDeltasWithoutMomentum() {
			for (int i = 0; i < this.deltaWeights.length; i++) {
				double oldWeight = this.inputWeights.get(i).value;
				double newWeight = oldWeight
						- NeuralNet.this.LEARNING_RATE * (this.deltaWeights[i] / this.nSamples);
				this.inputWeights.get(i).value = newWeight;
				this.deltaWeights[i] = 0.0;
				if (Double.isNaN(this.inputWeights.get(i).value)) {
					this.inputWeights.get(i).value = randgen.nextDouble()
							/ this.inputWeights.size();
					System.out.println("ERROR: Weight is NaN. Aborting...");
					System.exit(1);
				}
			}
			this.nSamples = 0;
		}


		/**
		 * Propagates the error term from this neuron backward to other
		 * connected neurons.
		 */
		public void backPropagate() {
			for (int i = 0; i < this.inputNeurons.size(); i++) {
				Neuron n = this.inputNeurons.get(i);
				n.setWeightedErrorSum(n.getWeightedErrorSum()
						+ (this.errorTerm * this.inputWeights.get(i).value));


				double newWeight = this.inputWeights.get(i).value;
				newWeight -= NeuralNet.this.LEARNING_RATE * (+0.000 * newWeight);
				// this.inputWeights.set(i, new
				// Double(newWeight));
				this.deltaWeights[i] += this.errorTerm * n.getOutputValue();
			}
			this.nSamples++;
		}


		public void backPropagate(double gamma) {
			for (int i = 0; i < this.inputNeurons.size(); i++) {
				Neuron n = this.inputNeurons.get(i);
				n.setWeightedErrorSum(n.getWeightedErrorSum()
						+ (this.errorTerm * this.inputWeights.get(i).value));

				this.deltaWeights[i] = gamma * this.deltaWeights[i]
						+ NeuralNet.this.LEARNING_RATE * (this.errorTerm * n.getOutputValue());
			}
			this.nSamples++;
		}


		public double getErrorTerm() {
			return this.errorTerm;
		}


		/**
		 * Returns the output value of this neuron. The output value is
		 * no recalculated when calling this function. To recalculate
		 * the output, see {@code recalcOutput}.
		 * 
		 * @see #recalcOutput
		 */
		public double getOutputValue() {
			return this.outputValue;
		}


		private double getWeightedErrorSum() {
			return this.weightedErrorSum;
		}


		public double getWeightedSumOfInputs() {
			return this.weightedInputSum;
		}


		public void recalcErrorTerm() {
			this.errorTerm = this.weightedErrorSum
					* this.activeFunc.activationDerivative(this.weightedInputSum);
		}


		/**
		 * Recalculates this neuron's internally stored output value
		 * based on its input weights and input neurons
		 */
		public void recalcOutput() {
			// If there are no inputs, output shouldn't change
			if (this.inputNeurons.size() == 0) {
				return;
			}

			this.recalcWeightedSumOfInputs();
			this.outputValue = this.activeFunc.activationValue(this.getWeightedSumOfInputs());
			// if (this.outputValue < 0.5) {
			// this.outputValue = 0.1;
			// } else {
			// this.outputValue = 1;
			// }
		}


		public void recalcWeightedSumOfInputs() {
			this.weightedInputSum = 0.0;
			for (int i = 0; i < this.inputNeurons.size(); i++) {
				this.weightedInputSum += this.inputNeurons.get(i).getOutputValue()
						* this.inputWeights.get(i).value;
			}
		}


		public void resetBackPropagationTerms() {
			this.errorTerm = 0.0;
			this.weightedErrorSum = 0.0;
		}


		/**
		 * Manually set the output of this neuron, rather than
		 * calculating it from inputs.
		 * 
		 * @param value
		 *                the value to set
		 */
		public void setOutputValue(double value) {
			this.outputValue = value;
		}


		public void setErrorTerm(double errorTerm) {
			this.errorTerm = errorTerm;
		}


		private void setWeightedErrorSum(double weightedErrorSum) {
			this.weightedErrorSum = weightedErrorSum;
		}
	}
}


class EditableDouble {
	public double value = 0.0;


	public EditableDouble() {

	}


	public EditableDouble(double value) {
		this.value = value;
	}


	@Override
	public String toString() {
		return String.format("%f", this.value);
	}
}
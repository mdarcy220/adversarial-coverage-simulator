package deeplearning;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Random;

public class NeuralNet {
	List<List<Neuron>> layers = new ArrayList<>();

	final double LEARNING_RATE = 0.1;


	public NeuralNet(int[] nNodesInLayer) {
		// Set up the input layer
		this.layers.add(new ArrayList<Neuron>());
		for (int i = 0; i < nNodesInLayer[0]; i++) {
			this.layers.get(0).add(new Neuron());
		}
		// Add a bias neuron to the input layer
		this.layers.get(0).add(new Neuron());
		this.layers.get(0).get(this.layers.get(0).size() - 1).setOutputValue(1.0);

		for (int i = 1; i < nNodesInLayer.length; i++) {
			this.layers.add(new ArrayList<Neuron>());
			for (int j = 0; j < nNodesInLayer[i]; j++) {
				Neuron n = new Neuron();
				for (int k = 0; k < this.layers.get(i - 1).size(); k++) {
					n.addInput(this.layers.get(i - 1).get(k));
				}
				this.layers.get(i).add(n);
			}
			// Add a bias neuron to the layer, if it isn't the
			// output layer
			if (i < nNodesInLayer.length - 1) {
				this.layers.get(i).add(new Neuron());
				this.layers.get(i).get(this.layers.get(i).size() - 1).setOutputValue(1.0);
			}
		}

	}


	public static void main_(String[] args) {
		int[] nodesPerLayer = new int[3];
		nodesPerLayer[0] = 14;
		nodesPerLayer[1] = 20;
		nodesPerLayer[2] = 7;
		NeuralNet nn = new NeuralNet(nodesPerLayer);
		double[] inputs1 = new double[14];
		// inputs1[0] = 1;
		// inputs1[1] = 1;

		Random randgen = new Random();
		for (int i = 0; i < 900000; i++) {
			byte num1 = (byte) randgen.nextInt(63);
			byte num2 = (byte) randgen.nextInt(63);
			System.arraycopy(byteToDoubleBinaryArray(num1), 0, inputs1, 0, 7);
			System.arraycopy(byteToDoubleBinaryArray(num2), 0, inputs1, 7, 7);
			double[] correctOut = byteToDoubleBinaryArray((byte) (num1 + num2));
			nn.learnFromExample(inputs1, correctOut);
		}


		byte num1 = 4;
		byte num2 = 4;
		System.arraycopy(byteToDoubleBinaryArray(num1), 0, inputs1, 0, 7);
		System.arraycopy(byteToDoubleBinaryArray(num2), 0, inputs1, 7, 7);
		nn.feedForward(inputs1);
		double[] out1 = nn.getOutputs();
		for (int i = 0; i < out1.length; i++) {
			System.out.printf("Output #%d: %f\n", i, out1[i]);
		}
		
	}


	private static double[] byteToDoubleBinaryArray(byte in) {
		double[] bindata = new double[7];
		for (byte i = 6; 0 <= i; i--) {
			bindata[i] = 0 < (in & (1 << i)) ? 1 : 0;
		}
		return bindata;
	}


	private static double sigmoid(double x) {
		return (1 / (1 + Math.pow(Math.E, -x)));
	}


	private static double sigmoidDerivative(double x) {
		return sigmoid(x) * (1 - sigmoid(x));
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


	public double[] getOutputs() {
		// Make an array with the same size as the last layer of the
		// network
		double[] outputs = new double[this.layers.get(this.layers.size() - 1).size()];

		for (int i = 0; i < outputs.length; i++) {
			outputs[i] = this.layers.get(this.layers.size() - 1).get(i).getOutputValue();
		}

		return outputs;
	}


	public void learnFromExample(double[] inputs, double[] correctOutputs) {

		for (int i = 0; i < this.layers.size(); i++) {
			for (int j = 0; j < this.layers.get(i).size(); j++) {
				this.layers.get(i).get(j).resetBackPropagationTerms();
			}
		}
		///System.out.println("Input1 = " + inputs[0] + " Input2 = " + inputs[1]);
		feedForward(inputs);
		double[] realOutputs = this.getOutputs();
		//System.out.println("Outputs[0]=" + realOutputs[0]);
		List<Neuron> outputNeurons = this.layers.get(this.layers.size() - 1);
		for (int i = 0; i < outputNeurons.size(); i++) {
			outputNeurons.get(i).setErrorTerm(-(correctOutputs[i] - realOutputs[i])
					* sigmoidDerivative(outputNeurons.get(i).getWeightedSumOfInputs()));
			outputNeurons.get(i).backPropagate();
		}
		for (int i = this.layers.size() - 2; 0 < i; i--) {
			for (int j = 0; j < this.layers.get(i).size(); j++) {
				this.layers.get(i).get(j).recalcErrorTerm();
				this.layers.get(i).get(j).backPropagate();

			}
		}
	}

	/**
	 * Represents an artificial neuron
	 * 
	 * @author Mike D'Arcy
	 *
	 */
	class Neuron {
		List<Neuron> inputNeurons = new ArrayList<>();
		List<Double> inputWeights = new ArrayList<>();
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


		public void addInput(Neuron n) {
			this.inputNeurons.add(n);
			this.inputWeights.add(new Double(Math.random()));
		}


		/**
		 * Propagates the error term from this neuron backward to other
		 * connected neurons.
		 */
		public void backPropagate() {
			for (int i = 0; i < this.inputNeurons.size(); i++) {
				Neuron n = this.inputNeurons.get(i);
				n.setWeightedErrorSum(
						n.getWeightedErrorSum() + (this.errorTerm * this.inputWeights.get(i)));


				double newWeight = this.inputWeights.get(i);
				newWeight -= NeuralNet.this.LEARNING_RATE
						* (this.errorTerm * n.getOutputValue() + 0.0 * n.getOutputValue());
				this.inputWeights.set(i, new Double(newWeight));
			}
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
			this.errorTerm = this.weightedErrorSum * sigmoidDerivative(this.weightedInputSum);
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
			this.outputValue = sigmoid(this.getWeightedSumOfInputs());
//			if (this.outputValue < 0.5) {
//				this.outputValue = 0.1;
//			} else {
//				this.outputValue = 1;
//			}
		}


		public void recalcWeightedSumOfInputs() {
			this.weightedInputSum = 0.0;
			for (int i = 0; i < this.inputNeurons.size(); i++) {
				this.weightedInputSum += this.inputNeurons.get(i).getOutputValue()
						* this.inputWeights.get(i).doubleValue();
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

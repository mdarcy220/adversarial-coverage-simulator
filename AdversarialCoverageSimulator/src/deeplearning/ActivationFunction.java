package deeplearning;

/**
 * Represents and activation function
 * 
 * @author Mike D'Arcy
 *
 */
public abstract class ActivationFunction {
	public static final ActivationFunction RELU_ACTIVATION = new ActivationFunction() {

		@Override
		public double activationValue(double x) {
			return reLU(x);
		}


		@Override
		public double activationDerivative(double x) {
			return reLUDerivative(x);
		}

	};
	public static final ActivationFunction LINEAR_ACTIVATION = new ActivationFunction() {

		@Override
		public double activationValue(double x) {
			return x;
		}


		@Override
		public double activationDerivative(double x) {
			return 1;
		}

	};
	public static final ActivationFunction SIGMOID_ACTIVATION = new ActivationFunction() {

		@Override
		public double activationValue(double x) {
			return sigmoid(x);
		}


		@Override
		public double activationDerivative(double x) {
			return sigmoidDerivative(x);
		}

	};
	
	private static double activationFunc(double x) {
		// return sigmoid(x);
		return reLU(x);
	}


	private static double activationFuncDerivative(double x) {
		// return sigmoidDerivative(x);
		return reLUDerivative(x);
	}


	private static double activationFuncDerivative(double x, double lastOutput) {
		// return fastSigmoidDerivative(lastOutput);
		return reLUDerivative(x);
	}


	private static double sigmoid(double x) {
		return (1 / (1 + Math.pow(Math.E, -x)));
	}


	private static double sigmoidDerivative(double x) {
		double d = sigmoid(x);
		return d * (1 - d);
	}


	//private static double fastSigmoidDerivative(double sigmoidx) {
	//	return sigmoidx * (1 - sigmoidx);
	//}


	private static double reLU(double x) {
		return Math.max(0.0, x);
	}


	private static double reLUDerivative(double x) {
		if (x <= 0) {
			return 0.01;
		}
		return 1;
	}


	public abstract double activationValue(double x);


	public abstract double activationDerivative(double x);
}
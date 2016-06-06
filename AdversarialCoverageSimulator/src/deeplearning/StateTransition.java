package deeplearning;

public class StateTransition {
	public boolean isTerminal;
	public double[] nnInput;
	// double[] resultState;
	public double reward;
	public double correctQVal;
	public int action;
	public double[] nextInput;


	public StateTransition(double[] state) {
		this.nnInput = state;
	}


	public StateTransition() {
		// Empty constructor
	}
}

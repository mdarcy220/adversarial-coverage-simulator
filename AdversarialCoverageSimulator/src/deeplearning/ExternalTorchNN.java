package deeplearning;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.InputMismatchException;
import java.util.Scanner;

import adversarialcoverage.AdversarialCoverage;
import adversarialcoverage.TerminalCommand;

import java.io.BufferedReader;
import java.io.*;

/**
 * A Neural Network class, implemented with an external program that can be communicated
 * with via a pipe.
 * 
 * @author Mike D"Arcy
 *
 */
public class ExternalTorchNN extends NeuralNet {
	String outFilename;
	String inFilename;
	PrintWriter outWriter = new PrintWriter(System.out);
	Scanner inReader = new Scanner(System.in);
	BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
	StringBuilder outMsg = new StringBuilder("");


	public ExternalTorchNN(String outFile, String inFile) {
		this.registerCustomCommands();

		this.outFilename = outFile;
		this.inFilename = inFile;

		try {
			this.outWriter = new PrintWriter(new File(this.outFilename));
		} catch (FileNotFoundException e) {
			System.err.printf("Failed to find file %s. Using STDOUT instead.\n", this.outFilename);
		}

		try {
			this.br = new BufferedReader(new FileReader(this.inFilename));
		} catch (FileNotFoundException e) {
			System.err.printf("Failed to find file %s. Using STDIN instead.\n", this.inFilename);
		}
	}


	private void registerCustomCommands() {
		AdversarialCoverage.controller.registerCommand(":ExternalTorchNN_sendCommand", new TerminalCommand() {
			@Override
			public void execute(String[] args) {
				if (args.length < 1) {
					return;
				}
				sendCommand(args[0]);
			}
		});
	}


	private void sendCommand(String code) {
		this.outWriter.printf("%s\n", code);
		this.outWriter.flush();
	}


	@Override
	public void feedForward(double[] inputs) {
		this.outMsg.append("f\n");
		for (int i = 0; i < inputs.length; i++) {
			this.outMsg.append(String.format("%a ", inputs[i]));
		}

		this.outMsg.append('\n');
		this.outWriter.print(this.outMsg.toString());
		this.outWriter.flush();
		this.outMsg.setLength(0);
	}


	@Override
	public double[] getOutputs() {
		// Make an array with the same size as the last layer of the
		// network
		double[] outputs = new double[5];
		Scanner s = null;
		String str = "";
		try {
			str = this.br.readLine();
			if (str != null) {
				s = new Scanner(str);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (s == null) {
			return null;
		}

		try {
			for (int i = 0; i < outputs.length; i++) {
				outputs[i] = s.nextDouble();
			}
		} catch (InputMismatchException e) {
			System.err.printf("Input mismatch when attempting to retreive NN outputs!\n");
			System.err.printf("The offending input was encountered in this string: %s\n", str);
			try {
				Thread.sleep(500);
			} catch (InterruptedException e1) {
				System.err.println("Sleeping Thread was interrupted.");
				;
			}
		}

		return outputs;
	}


	@Override
	public void backPropagateFromLastSample_RMSProp(double[] correctOutputs) {

		this.outMsg.append("b\n");
		for (int i = 0; i < correctOutputs.length; i++) {
			this.outMsg.append(String.format("%a ", correctOutputs[i]));
		}
		this.outMsg.append('\n');
		this.outWriter.print(this.outMsg.toString());
		this.outWriter.flush();
		this.outMsg.setLength(0);
	}


	public void sendTransition(StateTransition trans, boolean allowPartial) {

		if (allowPartial) {
			this.outMsg.append("t_nostartstate\n");
		} else {
			this.outMsg.append("t\n");

			// Output the initial state
			for (int i = 0; i < trans.nnInput.length; i++) {
				this.outMsg.append(String.format("%a ", trans.nnInput[i]));
			}

			this.outMsg.append('\n');
		}

		// Output the transition info
		this.outMsg.append(String.format("%d %a %d \n", trans.action + 1, trans.reward, trans.isTerminal ? 1 : 0));

		if (!trans.isTerminal) {
			// Output the next state
			for (int i = 0; i < trans.nextInput.length; i++) {
				this.outMsg.append(String.format("%a ", trans.nextInput[i]));
			}
			this.outMsg.append('\n');
		}

		this.outWriter.print(this.outMsg.toString());
		this.outWriter.flush();
		this.outMsg.setLength(0);
	}


	public void runTorchMinibatch() {
		this.outWriter.println(AdversarialCoverage.settings.getString("neuralnet.torch.minibatch_code"));
		this.outWriter.flush();
	}


	@Override
	public void forget() {
		this.outWriter.println("forget");
		this.outWriter.flush();
	}
}

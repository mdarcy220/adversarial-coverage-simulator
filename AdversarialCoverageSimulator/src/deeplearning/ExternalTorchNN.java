package deeplearning;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Scanner;
import java.io.BufferedReader;
import java.io.*;

public class ExternalTorchNN extends NeuralNet {
	String outFile;
	String inFile;
	PrintWriter outWriter = new PrintWriter(System.out);
	Scanner inReader = new Scanner(System.in);
	BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
	StringBuilder outMsg = new StringBuilder("");


	public ExternalTorchNN(String outFile, String inFile) {
		this.outFile = outFile;
		this.inFile = inFile;
		try {
			this.outWriter = new PrintWriter(new File(this.outFile));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		try {
			// s = new Scanner(new File(this.inFile));
			this.br = new BufferedReader(new FileReader(this.inFile));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
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
		// Scanner s = new Scanner(System.in);
		
		// Make an array with the same size as the last layer of the
		// network
		double[] outputs = new double[5];
		Scanner s = null;
		try {
			String str = this.br.readLine();
			if(str != null) {
				s = new Scanner(str);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		if(s == null) {
			return outputs;
		}
		
		for (int i = 0; i < outputs.length; i++) {
			outputs[i] = s.nextDouble();
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
	
	public void sendTransition(double[] correctOutputs) {
		
		this.outMsg.append("b\n");
		for (int i = 0; i < correctOutputs.length; i++) {
			this.outMsg.append(String.format("%a ", correctOutputs[i]));
		}
		this.outMsg.append('\n');
		this.outWriter.print(this.outMsg.toString());
		this.outWriter.flush();
		this.outMsg.setLength(0);
	}
}

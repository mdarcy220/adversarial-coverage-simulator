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


	public ExternalTorchNN(String outFile, String inFile) {
		this.outFile = outFile;
		this.inFile = inFile;
		try {
			this.outWriter = new PrintWriter(new File(this.outFile));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


	@Override
	public void feedForward(double[] inputs) {
		this.outWriter.println("f");
		for (int i = 0; i < inputs.length; i++) {
			this.outWriter.printf("%.15f ", inputs[i]);
		}
		this.outWriter.println();
		this.outWriter.flush();
	}


	@Override
	public double[] getOutputs() {
		// Scanner s = new Scanner(System.in);
		BufferedReader br = null;
		try {
			// s = new Scanner(new File(this.inFile));
			br = new BufferedReader(new FileReader(this.inFile));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.exit(1);
		}
		// Make an array with the same size as the last layer of the
		// network
		double[] outputs = new double[5];
		Scanner s = null;
		try {
			String str = br.readLine();
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
		this.outWriter.println("b");
		for (int i = 0; i < correctOutputs.length; i++) {
			this.outWriter.printf("%.15f ", correctOutputs[i]);
		}
		this.outWriter.println();
		this.outWriter.flush();
	}
}

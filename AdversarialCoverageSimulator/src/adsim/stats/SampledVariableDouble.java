package adsim.stats;

public class SampledVariableDouble extends SampledVariable {
	protected double runningSum = 0.0;
	protected double maxVal = Double.NEGATIVE_INFINITY;
	protected double minVal = Double.POSITIVE_INFINITY;


	public SampledVariableDouble() {
		super();
	}


	public void addSample(double value) {
		this.runningSum += value;
		this.nSamples++;

		if (this.maxVal < value) {
			this.maxVal = value;
		}

		if (value < this.minVal) {
			this.minVal = value;
		}

		// Update variance params
		double delta = value - this.runningMean;
		this.runningMean += delta / this.nSamples;
		this.m2 += delta * (value - this.runningMean);
	}
	
	
	public double getMax() {
		return this.maxVal;
	}
	
	
	public double getMin() {
		return this.minVal;
	}


	public double mean() {
		return (this.runningSum / this.nSamples);
	}


	public double variance() {
		if (this.nSamples < 2) {
			return Double.NaN;
		}

		return (this.m2 / (this.nSamples - 1.0));
	}


	public double stddev() {
		return Math.sqrt(this.variance());
	}


	public double sum() {
		return this.runningSum;
	}


	@Override
	public void reset() {
		super.reset();
		this.runningSum = 0.0;
		this.maxVal = 0.0;
		this.minVal = 0.0;
	}
}

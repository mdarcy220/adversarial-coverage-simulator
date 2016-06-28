package adversarialcoverage.stats;

public class SampledVariableDouble extends SampledVariable {
	protected double runningSum = 0.0;


	public SampledVariableDouble() {
		super();
	}


	public void addSample(double value) {
		this.runningSum += value;
		this.nSamples++;

		// Update variance params
		double delta;
		// Params for step count
		delta = value - this.runningMean;
		this.runningMean += delta / this.nSamples;
		this.m2 += delta * (value - this.runningMean);
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
	}
}

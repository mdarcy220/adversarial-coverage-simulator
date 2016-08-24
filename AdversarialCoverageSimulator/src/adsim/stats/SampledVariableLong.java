package adsim.stats;

public class SampledVariableLong extends SampledVariable {
	protected long runningSum = 0;

	protected long maxVal = Long.MIN_VALUE;
	protected long minVal = Long.MAX_VALUE;


	public SampledVariableLong() {
		this.reset();
	}


	public void addSample(long value) {
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


	public long getMax() {
		return this.maxVal;
	}


	public long getMin() {
		return this.minVal;
	}


	/**
	 * Gets the mean of the samples
	 * 
	 * @return the mean
	 */
	public double mean() {
		if (this.nSamples < 1) {
			return Double.NaN;
		}
		return (((double) this.runningSum) / ((double) this.nSamples));
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


	public long sum() {
		return this.runningSum;
	}


	@Override
	public void reset() {
		super.reset();
		this.runningSum = 0;
		this.maxVal = 0;
		this.minVal = 0;
	}
}

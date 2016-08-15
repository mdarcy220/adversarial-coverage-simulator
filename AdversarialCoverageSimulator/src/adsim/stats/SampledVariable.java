package adsim.stats;

public abstract class SampledVariable {
	protected long nSamples = 0;
	protected double runningMean = 0.0;
	protected double m2 = 0.0;


	public SampledVariable() {
		this.reset();
	}


	public long numSamples() {
		return this.nSamples;
	}


	public void reset() {
		this.nSamples = 0;
		this.runningMean = 0.0;
		this.m2 = 0.0;
	}
}

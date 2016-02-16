import java.awt.image.BufferedImage;

public abstract class Robot {
	CoverageAlgorithm coverAlgo;
	BufferedImage sprite;
	/**
	 * The uniqueId is used to differentiate this robot from other robots in the environment
	 */
	int uniqueId = 0;
	
	/**
	 * Whether the robot can continue coverage or not
	 */
	boolean isBroken = false;
	
	protected Robot() {
		this.uniqueId = 0;
	}
	
	protected Robot(int uniqueId) {
		this.uniqueId = uniqueId;
	}
	
	public int getId() {
		return this.uniqueId;
	}
	
	/**
	 * Check whether the robot can continue coverage
	 * @return
	 */
	public boolean isBroken() {
		return this.isBroken;
	}

	public void setBroken(boolean broken) {
		this.isBroken = broken;
	}
}

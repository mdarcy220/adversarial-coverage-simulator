package gridenv;

import adsim.Coordinate;
import adsim.Robot;
import algo.coverage.GridCoverageAlgorithm;

/**
 * A robot spcialized for grid environments.
 * 
 * @author Mike D'Arcy
 *
 */
public class GridRobot extends Robot {
	private Coordinate location;


	/**
	 * Constructs a new grid-based robot at the specified location, with the
	 * specified unique id.
	 * 
	 * @param uniqueId
	 *                the robot's id, used to identify it
	 * @param x
	 *                the starting x coordinate of the robot
	 * @param y
	 *                the starting y coordinate of the robot
	 */
	public GridRobot(int uniqueId, int x, int y) {
		super(uniqueId);
		this.location = new Coordinate(x, y);
	}


	/**
	 * Get this robot's (x, y) location
	 * 
	 * @return
	 */
	public Coordinate getLocation() {
		return this.location;
	}


	/**
	 * Set this robot's (x, y) location
	 * 
	 * @param x
	 * @param y
	 */
	public void setLocation(int x, int y) {
		this.location.x = x;
		this.location.y = y;
	}


	/**
	 * Sets this robot's coverage algorithm
	 * 
	 * @param algo
	 */
	public void setCoverageAlgorithm(GridCoverageAlgorithm algo) {
		this.coverAlgo = algo;
	}


	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((this.location == null) ? 0 : this.location.hashCode());
		return result;
	}


	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		GridRobot other = (GridRobot) obj;
		if (this.location == null) {
			if (other.location != null)
				return false;
		} else if (!this.location.equals(other.location))
			return false;
		return true;
	}
}
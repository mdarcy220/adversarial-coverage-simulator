package adversarialcoverage;
/**
 * This class represents an (x, y) coordinate pair in 2D space.
 * 
 * @author Mike D'Arcy
 *
 */
class Coordinate {
	int x;
	int y;


	/**
	 * Creates a new coordinate representing (0, 0)
	 */
	public Coordinate() {
		this(0, 0);
	}


	/**
	 * Creates a new coordinate representing the given x and y.
	 * 
	 * @param x
	 * @param y
	 */
	public Coordinate(int x, int y) {
		this.x = x;
		this.y = y;
	}


	/**
	 * Returns the Manhattan distance between this coordinate and the given
	 * coordinate.
	 * 
	 * @param c
	 *                the other coordinate
	 * @return the Manhattan distance between the coordinates
	 */
	public int manhattanDistance(Coordinate c) {
		return Math.abs(this.x - c.x) + Math.abs(this.y - c.y);
	}


	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + this.x;
		result = prime * result + this.y;
		return result;
	}


	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		Coordinate other = (Coordinate) obj;
		if (this.x != other.x) {
			return false;
		}
		if (this.y != other.y) {
			return false;
		}
		return true;
	}


	@Override
	public Coordinate clone() {
		return new Coordinate(this.x, this.y);
	}
}


/**
 * Represents a node that is part of a grid.
 * 
 * @author Mike D'Arcy
 *
 */
public class GridNode extends Node {
	/**
	 * The location of the node within the grid.
	 */
	private Coordinate location;

	/**
	 * Cost of moving to this node
	 */
	double cost = 0.0;


	/**
	 * Create a new node
	 * 
	 * @param x
	 *                the x coordinate of the node
	 * @param y
	 *                the y coordinate of the node
	 * @param nodeType
	 *                the type of the node
	 */
	public GridNode(int x, int y, NodeType nodeType) {
		this.location = new Coordinate(x, y);
		this.nodeType = nodeType;
	}


	public double getCost() {
		return this.cost;
	}


	public void setCost(double cost) {
		this.cost = cost;
	}


	/**
	 * Create a clone of the node
	 */
	@Override
	public GridNode clone() {
		GridNode clone = new GridNode(this.location.x, this.location.y, this.nodeType);
		clone.setCoverCount(this.coverCount);
		clone.setCost(this.cost);
		clone.setDangerProb(this.dangerProb);
		return clone;
	}


	public int getX() {
		return this.location.x;
	}


	public int getY() {
		return this.location.y;
	}


	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		long temp;
		temp = Double.doubleToLongBits(cost);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + ((location == null) ? 0 : location.hashCode());
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
		GridNode other = (GridNode) obj;
		if (Double.doubleToLongBits(cost) != Double.doubleToLongBits(other.cost)) {
			return false;
		}
		if (location == null) {
			if (other.location != null) {
				return false;
			}
		} else if (!location.equals(other.location)) {
			return false;
		}
		return true;
	}
}


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
		result = prime * result + x;
		result = prime * result + y;
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
		if (x != other.x) {
			return false;
		}
		if (y != other.y) {
			return false;
		}
		return true;
	}


	@Override
	public Coordinate clone() {
		return new Coordinate(this.x, this.y);
	}
}

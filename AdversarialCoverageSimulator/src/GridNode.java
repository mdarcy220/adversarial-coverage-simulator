
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
	 *            the x coordinate of the node
	 * @param y
	 *            the y coordinate of the node
	 * @param nodeType
	 *            the type of the node
	 */
	public GridNode(int x, int y, NodeType nodeType) {
		this.location = new Coordinate(x, y);
		this.nodeType = nodeType;
	}

	/**
	 * Create a clone of the node
	 */
	@Override
	public GridNode clone() {
		GridNode clone = new GridNode(this.location.x, this.location.y, this.nodeType);
		clone.setCoverCount(this.coverCount);
		clone.setDangerProb(this.dangerProb);
		return clone;
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
	 *            the other coordinate
	 * @return the Manhattan distance between the coordinates
	 */
	public int manhattanDistance(Coordinate c) {
		return Math.abs(this.x - c.x) + Math.abs(this.y - c.y);
	}

	@Override
	public Coordinate clone() {
		return new Coordinate(this.x, this.y);
	}
}
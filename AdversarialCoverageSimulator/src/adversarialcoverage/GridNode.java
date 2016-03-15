package adversarialcoverage;

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
		temp = Double.doubleToLongBits(this.cost);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + ((this.location == null) ? 0 : this.location.hashCode());
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
		if (Double.doubleToLongBits(this.cost) != Double.doubleToLongBits(other.cost)) {
			return false;
		}
		if (this.location == null) {
			if (other.location != null) {
				return false;
			}
		} else if (!this.location.equals(other.location)) {
			return false;
		}
		return true;
	}
}




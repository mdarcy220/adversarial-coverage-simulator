package gridenv;

import adsim.Node;
import adsim.NodeType;

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
	protected Coordinate location;

	/**
	 * Cost of moving to this node
	 */
	protected double cost = 0.0;


	/**
	 * The number of times this node was covered by a robot
	 */
	protected int coverCount = 0;


	/**
	 * The "spreadability" of danger
	 */
	public double spreadability = 0.0;


	/**
	 * The amount of "fuel" the danger has (think of a fire). If the fuel reaches
	 * zero, the danger will begin to decrease.
	 */
	public double dangerFuel = 0.0;


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


	/**
	 * Create a clone of the node
	 */
	@Override
	public GridNode clone() {
		GridNode clone = new GridNode(this.location.x, this.location.y, this.nodeType);
		clone.setCost(this.cost);
		clone.setDangerProb(this.dangerProb);
		return clone;
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


	public double getCost() {
		return this.cost;
	}


	/**
	 * Gets the cover count
	 * 
	 * @return the number of times this node has been covered
	 */
	public int getCoverCount() {
		return this.coverCount;
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


	/**
	 * Increment the cover count of this node
	 */
	public void incrementCoverCount() {
		this.coverCount++;
	}


	public void setCost(double cost) {
		this.cost = cost;
	}


	/**
	 * Set the cover count
	 * 
	 * @param count
	 */
	public void setCoverCount(int count) {
		this.coverCount = count;
	}
}


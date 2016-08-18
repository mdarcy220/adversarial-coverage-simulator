package adsim;

public class Node {
	/**
	 * The type of node this is (obstacle, free, etc)
	 */
	protected NodeType nodeType;

	/**
	 * The chance of something bad happening to a robot that moves to this node.
	 */
	protected double dangerProb = 0.0;


	/**
	 * Get the node type
	 * 
	 * @return the {@code NodeType} of this node
	 */
	public NodeType getNodeType() {
		return this.nodeType;
	}


	/**
	 * Set the node type
	 * 
	 * @param nodeType
	 * @see NodeType
	 */
	public void setNodeType(NodeType nodeType) {
		this.nodeType = nodeType;
	}


	/**
	 * Set the probability of a robot being stopped when it lands on this node
	 * 
	 * @param dangerProb
	 *                the new probability (between 0 and 1)
	 */
	public void setDangerProb(double dangerProb) {
		this.dangerProb = dangerProb;
	}


	/**
	 * Gets the probability of a robot being stopped upon landing in this node.
	 * 
	 * @return
	 */
	public double getDangerProb() {
		return this.dangerProb;
	}


	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		long temp;
		temp = Double.doubleToLongBits(this.dangerProb);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + ((this.nodeType == null) ? 0 : this.nodeType.hashCode());
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
		Node other = (Node) obj;
		if (Double.doubleToLongBits(this.dangerProb) != Double.doubleToLongBits(other.dangerProb)) {
			return false;
		}
		if (this.nodeType != other.nodeType) {
			return false;
		}
		return true;
	}

}

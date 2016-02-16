
public class Node {
	/**
	 * The number of times this node was covered by a robot
	 */
	protected int coverCount = 0;
	/**
	 * The type of node this is (obstacle, free, etc)
	 */
	protected NodeType nodeType;

	/**
	 * The chance of something bad happening to a robot that moves to this node.
	 */
	double dangerProb = 0.0;

	/**
	 * Increment the cover count of this node
	 */
	public void incrementCoverCount() {
		this.coverCount++;
	}

	/**
	 * Set the cover count
	 * 
	 * @param count
	 */
	public void setCoverCount(int count) {
		this.coverCount = count;
	}

	/**
	 * Gets the cover count
	 * 
	 * @return the number of times this node has been covered
	 */
	public int getCoverCount() {
		return this.coverCount;
	}

	/**
	 * Get the node type
	 * 
	 * @return the {@code NodeType} of this node
	 */
	public NodeType getNodeType() {
		return nodeType;
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
	 *            the new probability (between 0 and 1)
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

}
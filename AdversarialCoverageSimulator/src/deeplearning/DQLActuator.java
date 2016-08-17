/**
 * 
 */
package deeplearning;

import adsim.Actuator;

/**
 * @author Mike D;Arcy
 *
 */
public interface DQLActuator extends Actuator {
	/**
	 * Gets the reward received from taking the last action. This is necessary for
	 * Q-Learning.
	 * 
	 * @param action
	 *                the id of the action to be taken
	 * @return the reward
	 */
	public double getLastReward();
}

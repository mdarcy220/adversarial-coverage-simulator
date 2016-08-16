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
	 * Gets the reward for taking the specified action.
	 * 
	 * @param action
	 *                the id of the action to be taken
	 * @return the reward
	 */
	public double getLastReward();
}

/**
 * 
 */
package adsim;

/**
 * 
 * 
 * @author Mike D/Arcy
 *
 */
public interface Actuator {
	/**
	 * Takes the action specified by the actionId and updates the environment
	 * accordingly
	 * 
	 * @param actionId
	 *                the ID of the action to take
	 */
	public void takeActionById(int actionId);


	/**
	 * Gets the ID of the last action taken.
	 * 
	 * @return the id of the last action
	 */
	public int getLastActionId();


	/**
	 * Reloads internal settings to match global settings.
	 */
	public void reloadSettings();
}

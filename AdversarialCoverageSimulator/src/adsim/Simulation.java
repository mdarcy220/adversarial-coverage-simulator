package adsim;

import gridenv.GridEnvironment;

public interface Simulation {
	/**
	 * Initializer for the simulation.
	 */
	public void init();


	/**
	 * Sets the environment for the simulation.
	 * 
	 * @param env
	 *                - the new environment
	 */
	public void setEnvironment(GridEnvironment newEnv);


	/**
	 * Event hook for init of the environment
	 */
	public void onEnvInit();


	/**
	 * Event hook for reaching an end state.
	 */
	public void onRunEnd();


	/**
	 * Event hook for a simulation step.
	 */
	public void onStep();


	/**
	 * Checks if an end state has been reached
	 * 
	 * @return true if the current state is an end state, false otherwise
	 */
	public boolean isTerminalState();


	/**
	 * Refreshes internal settings according to the global settings.
	 */
	public void reloadSettings();
}

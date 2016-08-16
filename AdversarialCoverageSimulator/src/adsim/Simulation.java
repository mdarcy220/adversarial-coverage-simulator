package adsim;

/**
 * This interface defines a simulation. A simulation determines what should happen on each
 * event hook from the simulation engine, and provides the appropriate specialty helper
 * classes (such as displays and algorithms) necessary to run. A simulation must have a
 * way to check when a goal state is reached.
 * 
 * @author Mike D'Arcy
 *
 */
public interface Simulation {
	/**
	 * Initializer for the simulation.
	 */
	public void init();


	/**
	 * Checks if an end state has been reached
	 * 
	 * @return true if the current state is an end state, false otherwise
	 */
	public boolean isTerminalState();


	/**
	 * Event hook for init of the environment
	 */
	public void onEnvInit();


	/**
	 * Event hook for starting a new run of the simulation.
	 */
	public void onNewRun();


	/**
	 * Event hook for reaching an end state.
	 */
	public void onRunEnd();


	/**
	 * Event hook for a simulation step.
	 */
	public void onStep();


	/**
	 * Refreshes internal settings according to the global settings.
	 */
	public void reloadSettings();


	/**
	 * Sets the environment for the simulation.
	 * 
	 * @param env
	 *                - the new environment
	 */
	public void setEngine(SimulatorEngine simulatorEngine);
}

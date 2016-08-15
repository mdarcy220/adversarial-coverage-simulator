package simulations.generic;

import adsim.Simulation;
import gridenv.GridEnvironment;

/**
 * A simple dummy simulation class, with no goal.
 * 
 * @author Mike D'Arcy
 *
 */
public class GenericSimulation implements Simulation {

	GridEnvironment env = null;


	public GenericSimulation() {

	}


	@Override
	public void setEnvironment(GridEnvironment env) {
		this.env = env;
	}


	@Override
	public void onRunEnd() {

	}


	@Override
	public boolean isTerminalState() {
		return false;
	}


	@Override
	public void init() {

	}


	@Override
	public void onStep() {

	}


	@Override
	public void reloadSettings() {

	}


	@Override
	public void onEnvInit() {

	}

}

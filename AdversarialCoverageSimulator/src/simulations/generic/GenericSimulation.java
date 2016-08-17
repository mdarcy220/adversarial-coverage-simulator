package simulations.generic;

import adsim.Simulation;
import adsim.SimulatorEngine;
import gridenv.GridEnvironment;

/**
 * A simple dummy simulation class, with no goal.
 * 
 * @author Mike D'Arcy
 *
 */
public class GenericSimulation implements Simulation {

	GridEnvironment env = null;
	SimulatorEngine engine = null;


	public GenericSimulation() {

	}


	@Override
	public void init() {

	}


	@Override
	public boolean isTerminalState() {
		return false;
	}


	@Override
	public void onEnvInit() {

	}


	@Override
	public void onNewRun() {

	}


	@Override
	public void onRunEnd() {

	}


	@Override
	public void onStep() {

	}


	@Override
	public void reloadSettings() {

	}


	@Override
	public void setEngine(SimulatorEngine engine) {
		this.engine = engine;
	}


	@Override
	public void dispose() {
		// TODO Auto-generated method stub
		
	}

}

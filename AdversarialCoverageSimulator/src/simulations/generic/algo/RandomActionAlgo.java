package simulations.generic.algo;

import adsim.Actuator;
import adsim.Algorithm;
import gridenv.GridSensor;

/**
 * A coverage algorithm that covers the grid using an e-greedy policy, moving either
 * randomly or taking the action with highest immediate reward (i.e., looking only one
 * step into the future).
 * 
 * @author Mike D'Arcy
 *
 */
public class RandomActionAlgo implements Algorithm {
	GridSensor sensor;
	Actuator actuator;
	long stepNum = 0;


	public RandomActionAlgo(GridSensor sensor, Actuator actuator) {
		this.sensor = sensor;
		this.actuator = actuator;
	}


	@Override
	public void init() {

	}


	@Override
	public void step() {
		int direction = (int) (Math.random() * this.actuator.getNumActions());
		this.actuator.takeActionById(direction);
		this.stepNum++;
	}


	@Override
	public void reloadSettings() {

	}

}

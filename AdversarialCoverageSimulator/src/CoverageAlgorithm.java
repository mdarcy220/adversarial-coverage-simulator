abstract class CoverageAlgorithm {
	Sensor sensor;
	Actuator actuator;

	public abstract void init();
	public abstract void step();
}
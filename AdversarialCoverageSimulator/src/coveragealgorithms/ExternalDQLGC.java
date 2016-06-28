package coveragealgorithms;

import adversarialcoverage.AdversarialCoverage;
import adversarialcoverage.GridActuator;
import adversarialcoverage.GridSensor;
import deeplearning.ExternalTorchNN;
import deeplearning.StatePreprocessor;
import deeplearning.StateTransition;

public class ExternalDQLGC implements GridCoverageAlgorithm {
	private GridSensor sensor;
	private GridActuator actuator;
	private GridCoverageAlgorithm realCoverageAlgo;
	private ExternalTorchNN nn = null;
	private boolean ALLOW_PARTIAL_TRANSITIONS = AdversarialCoverage.settings.getBoolean("neuralnet.torch.use_partial_transitions");
	private StatePreprocessor preprocessor;
	private StateTransition transition = new StateTransition();


	/**
	 * Constructs a new deep q-learner (which actually just sends transitions to an
	 * external process for deep q-learning). The "real" coverage algorithm passed to
	 * this constructor is the one that will actually execute the grid coverage. An
	 * important note is that, in order to get information about the actions and
	 * rewards from the other algorithm, the actuator given here MUST be the same one
	 * used by the other algorithm, or it must be an actuator whose lastReward and
	 * lastActionId are updated according to the other coverage algorithm.
	 * 
	 * @param sensor
	 *                the sensor to get information about the environment
	 * @param actuator
	 *                the actuator used by the real coverage algorithm (used to
	 *                retrieve reward and action info)
	 * @param realCoverageAlgo
	 *                the "real" coverage algorithm, used to choose all the actions
	 *                for the coverage
	 */
	public ExternalDQLGC(GridSensor sensor, GridActuator actuator, GridCoverageAlgorithm realCoverageAlgo) {
		this.sensor = sensor;
		this.actuator = actuator;
		this.preprocessor = new StatePreprocessor(this.sensor);
		this.realCoverageAlgo = realCoverageAlgo;
	}


	@Override
	public void reloadSettings() {
		this.actuator.reloadSettings();
		this.sensor.reloadSettings();
		this.preprocessor.reloadSettings();
		this.realCoverageAlgo.reloadSettings();

		this.ALLOW_PARTIAL_TRANSITIONS = AdversarialCoverage.settings.getBoolean("neuralnet.torch.use_partial_transitions");
	}


	@Override
	public void init() {
		this.transition.nnInput = this.preprocessor.createEmptyStateBuffer();
		this.transition.nextInput = this.preprocessor.createEmptyStateBuffer();
		String prefix = AdversarialCoverage.settings.getString("deepql.external_torch_nn.io_file_prefix");
		this.nn = new ExternalTorchNN(prefix + AdversarialCoverage.settings.getString("deepql.external_torch_nn.nninput_file_name"),
				prefix + AdversarialCoverage.settings.getString("deepql.external_torch_nn.nnoutput_file_name"));
		this.realCoverageAlgo.init();
	}


	@Override
	public void step() {
		this.transition.nnInput = this.preprocessor.getPreprocessedState(this.transition.nnInput);
		this.realCoverageAlgo.step();
		this.transition.action = this.actuator.getLastActionId();
		this.transition.reward = this.actuator.getLastReward();
		this.transition.isTerminal = this.sensor.isFinished();
		this.transition.nextInput = this.preprocessor.getPreprocessedState(this.transition.nextInput);

		this.nn.sendTransition(this.transition, this.ALLOW_PARTIAL_TRANSITIONS);
	}
}

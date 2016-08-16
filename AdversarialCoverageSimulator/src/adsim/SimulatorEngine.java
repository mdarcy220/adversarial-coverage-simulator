package adsim;

import simulations.coverage.CoverageStats;
import simulations.generic.GenericSimulation;

public class SimulatorEngine {

	private boolean isRunning = false;
	private DisplayAdapter display = null;
	private Thread simulationThread = null;
	private Simulation simulation;
	private static final DisplayAdapter EmptyDisplayAdapter = new DisplayAdapter() {
		@Override
		public void refresh() {
			// Do nothing
		}


		@Override
		public void dispose() {
			// Do nothing
		}
	};


	public SimulatorEngine(Simulation sim) {
		this.setDisplay(SimulatorEngine.EmptyDisplayAdapter);
		this.setSimulation(sim);
		this.init();
	}


	public SimulatorEngine() {
		this(new GenericSimulation());
	}


	public void init() {
		this.registerConsoleCommands();
	}


	private void registerConsoleCommands() {
		final ConsoleController controller = SimulatorMain.controller;

		controller.registerCommand(":pause", new TerminalCommand() {
			@Override
			public void execute(String[] args) {
				pauseSimulation();
			}
		});

		controller.registerCommand(":step", new TerminalCommand() {
			@Override
			public void execute(String[] args) {
				stepSimulation();
			}
		});

		controller.registerCommand(":run", new TerminalCommand() {
			@Override
			public void execute(String[] args) {
				runSimulation();
			}
		});

		controller.registerCommand(":new", new TerminalCommand() {
			@Override
			public void execute(String[] args) {
				newRun();
			}
		});

		controller.registerCommand(":showstate", new TerminalCommand() {
			@Override
			public void execute(String[] args) {
				System.out.printf("isRunning = %s, thread.isAlive() = %s\n", SimulatorEngine.this.isRunning ? "true" : "false",
						SimulatorEngine.this.simulationThread.isAlive() ? "true" : "false");
			}
		});

		controller.registerCommand(":quit", new TerminalCommand() {
			@Override
			public void execute(String[] args) {
				pauseSimulation();
				kill();
				controller.quit();
			}
		});

	}


	public void stepSimulation() {
		if (!this.simulation.isTerminalState()) {
			this.step();
		}
		refreshDisplay();
	}


	public void runSimulation() {
		this.isRunning = true;
		startSimulationLoop();
	}


	public void pauseSimulation() {
		this.isRunning = false;
	}


	public void threadSafePauseSimulation() {
		this.isRunning = false;
		try {
			if (this.isThreadRunning()) {
				this.simulationThread.join();
			}
		} catch (InterruptedException e) {
			System.err.println(
					"warn: Interrupted while waiting for simulation to finish! No guarantee of thread safety beyond this point.");
		}
	}


	public void newRun() {
		this.isRunning = false;
		this.simulation.onNewRun();
		refreshDisplay();
	}


	/**
	 * Updates the display, which may be a GUI window, or in a headless environment, a
	 * terminal.
	 */
	public void refreshDisplay() {
		this.display.refresh();
	}


	public void setDisplay(DisplayAdapter newDisplay) {
		if (this.display != null) {
			this.display.dispose();
		}

		if (newDisplay != null) {
			this.display = newDisplay;
		} else {
			this.display = SimulatorEngine.EmptyDisplayAdapter;
		}
	}


	public void setSimulation(Simulation newSimulation) {
		if (newSimulation == null) {
			setSimulation(new GenericSimulation());
			return;
		}

		if (this.isRunning) {
			System.out.println("warn: Simulation was running. Attempting to stop it...");
			this.pauseSimulation();
		}
		try {
			if (this.isThreadRunning()) {
				this.simulationThread.join();
			}
		} catch (InterruptedException e) {
			System.err.println(
					"warn: Interrupted while waiting for simulation to finish! No guarantee of thread safety beyond this point.");
		}

		this.simulation = newSimulation;
		this.simulation.setEngine(this);
		this.simulation.init();
	}


	private void startSimulationLoop() {
		if (this.isThreadRunning()) {
			System.err.print("Coverage thread is already running. No action will be taken.\n");
			return;
		}

		this.simulationThread = new Thread() {
			@Override
			public void run() {
				simulationLoop();
			}
		};
		this.simulationThread.start();
	}


	private void simulationLoop() {

		// Update settings
		this.simulation.reloadSettings();

		long delay = SimulatorMain.settings.getInt("autorun.stepdelay");
		boolean doRepaint = SimulatorMain.settings.getBoolean("autorun.do_repaint");

		while (this.isRunning) {
			long time = System.currentTimeMillis();
			this.step();
			if (doRepaint && !SimulatorMain.args.HEADLESS) {
				refreshDisplay();
			}
			if (this.simulation.isTerminalState()) {
				handleSimulationCompletion();
			}

			time = System.currentTimeMillis() - time;
			if (time < delay) {
				try {
					Thread.sleep(delay - time);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}


	private void handleSimulationCompletion() {
		this.simulation.onRunEnd();
		CoverageStats stats = SimulatorMain.getStats();

		refreshDisplay();

		if (stats != null) {
			stats.startNewRun();
		}
	}


	private void step() {
		this.simulation.onStep();
		SimulatorMain.getStats().updateTimeStep();
	}


	public boolean isRunning() {
		return this.isRunning;
	}


	public boolean isThreadRunning() {
		return (this.simulationThread != null && this.simulationThread.isAlive());
	}


	public void kill() {
		this.isRunning = false;
		this.display.dispose();
	}


	public void reloadSettings() {
		if (this.simulation != null) {
			this.simulation.reloadSettings();
		}
	}


	public Simulation getSimulation() {
		return this.simulation;
	}

}

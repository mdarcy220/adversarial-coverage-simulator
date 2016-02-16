import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;

import javax.swing.*;

public class AdversarialCoverage {

	AdversarialCoverageSettings settings = new AdversarialCoverageSettings();
	GridEnvironment env = null;
	CoverageWindow cw = new CoverageWindow();
	CoveragePanel mainPanel;
	/**
	 * Keeps track of whether the coverage simulation is running
	 */
	boolean isRunning = false;

	/**
	 * Whether the user has quit the coverage
	 */
	boolean hasQuit = false;

	public AdversarialCoverage() {

		settings.setIntProperty("autorun.sleeptime", 50);

		resetCoverageEnvironment();

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				createAndShowGUI();
			}
		});
	}

	private void createAndShowGUI() {
		mainPanel = new CoveragePanel(this.env);
		// Set up the window
		cw.setVisible(true);
		mainPanel.setSize(500, 500);
		cw.add(mainPanel);
		JMenuBar menuBar = new JMenuBar();

		// File menu
		JMenu fileMenu = new JMenu("File");
		final JMenuItem loadSettingsMenuItem = new JMenuItem("Load settings from file...");
		loadSettingsMenuItem.setToolTipText("Load program settings from a file");
		loadSettingsMenuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent ev) {
				JFileChooser chooser = new JFileChooser();
				int status = chooser.showOpenDialog(cw);
				if (status == JFileChooser.APPROVE_OPTION) {
					File settingsFile = chooser.getSelectedFile();
					loadSettingsFromFile(settingsFile);
				}
			}
		});
		fileMenu.add(loadSettingsMenuItem);

		menuBar.add(fileMenu);

		// Run menu
		JMenu runMenu = new JMenu("Run");
		final JMenuItem runCoverageMenuItem = new JMenuItem("Autorun");
		final JMenuItem pauseCoverageMenuItem = new JMenuItem("Stop");
		final JMenuItem stepCoverageMenuItem = new JMenuItem("Step");
		final JMenuItem restartCoverageMenuItem = new JMenuItem("Restart");

		runCoverageMenuItem.setToolTipText("Automatically step until coverage is done");
		runCoverageMenuItem.setAccelerator(KeyStroke.getKeyStroke("F6"));
		runCoverageMenuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent ev) {
				isRunning = true;
				pauseCoverageMenuItem.setEnabled(true);
				runCoverageMenuItem.setEnabled(false);
				stepCoverageMenuItem.setEnabled(false);
				runCoverage();
			}
		});
		runMenu.add(runCoverageMenuItem);

		pauseCoverageMenuItem.setToolTipText("Pause the simulation (if running)");
		pauseCoverageMenuItem.setAccelerator(KeyStroke.getKeyStroke("F7"));
		pauseCoverageMenuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent ev) {
				isRunning = false;
				runCoverageMenuItem.setEnabled(true);
				stepCoverageMenuItem.setEnabled(true);
				pauseCoverageMenuItem.setEnabled(false);
			}
		});
		runMenu.add(pauseCoverageMenuItem);

		stepCoverageMenuItem.setToolTipText("Increment the coverage by one step");
		stepCoverageMenuItem.setAccelerator(KeyStroke.getKeyStroke("F8"));
		stepCoverageMenuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent ev) {
				if (!env.isFinished()) {
					env.step();
				}
				mainPanel.repaint();
			}
		});
		runMenu.add(stepCoverageMenuItem);

		restartCoverageMenuItem.setToolTipText("Reinitialize the coverage");
		restartCoverageMenuItem.setAccelerator(KeyStroke.getKeyStroke("Ctrl R"));
		restartCoverageMenuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent ev) {
				isRunning = false;
				runCoverageMenuItem.setEnabled(true);
				stepCoverageMenuItem.setEnabled(true);
				pauseCoverageMenuItem.setEnabled(false);
				resetCoverageEnvironment();
				mainPanel.repaint();
			}
		});
		runMenu.add(restartCoverageMenuItem);

		menuBar.add(runMenu);
		cw.setJMenuBar(menuBar);
	}

	/**
	 * Sets up the environment using the settings
	 */
	public void resetCoverageEnvironment() {
		this.env = new GridEnvironment(
				new Dimension(settings.getIntProperty("env.grid.width"), settings.getIntProperty("env.grid.height")));

		// Set up the coverage environment
		if (!this.settings.hasProperty("grid.dangervalues")
				|| this.settings.getStringProperty("grid.dangervalues") == "") {
			randomizeDangerLevels();
		} else {
			boolean hasAllValues = true;
			Scanner valuesScanner = new Scanner(this.settings.getStringProperty("grid.dangervalues"));
			for (int x = 0; x < env.getWidth(); x++) {
				for (int y = 0; y < env.getHeight(); y++) {
					if (valuesScanner.hasNext()) {
						double value = valuesScanner.nextDouble();
						this.env.getGridNode(x, y).setDangerProb(value);
					} else {
						System.err.println(
								"Missing values in grid.dangervalues setting. Switching to randomized danger levels.");
						hasAllValues = false;
						break;
					}
				}
			}
			if (!hasAllValues) {
				randomizeDangerLevels();
			}
		}

		// Set up the robots
		for (int i = 0; i < 5; i++) {
			GridRobot robot = new GridRobot(i, (int) (Math.random() * 10), (int) (Math.random() * 10));
			GridSensor sensor = new GridSensor(env, robot);
			GridActuator actuator = new GridActuator(env, robot);
			CoverageAlgorithm algo = new RandomGridCoverage(sensor, actuator);
			robot.coverAlgo = algo;
			env.addRobot(robot);
		}

		env.init();

		if (mainPanel != null) {
			mainPanel.setEnvironment(this.env);
		}

	}

	private void randomizeDangerLevels() {
		Random rand = new Random();
		rand.setSeed(System.currentTimeMillis());
		// Set up threats. Half the cells are safe, the other half have
		// probabilities between 0 and 0.05.
		for (int x = 0; x < env.getWidth(); x++) {
			for (int y = 0; y < env.getHeight(); y++) {
				env.getGridNode(x, y).setDangerProb(rand.nextDouble());
				if (env.getGridNode(x, y).getDangerProb() < 0.5) {
					env.getGridNode(x, y).setDangerProb(0.0);
				} else {
					env.getGridNode(x, y).setDangerProb((env.getGridNode(x, y).getDangerProb() - 0.5)/10.0);
				}
			}
		}
	}

	/**
	 * Loads settings from the given file.
	 * 
	 * @param file
	 *            the file containing setting information
	 */
	public void loadSettingsFromFile(File file) {
		Scanner input = null;
		try {
			input = new Scanner(file);
		} catch (FileNotFoundException e) {
			System.err.println("Settings file not found.");
			return;
		}

		this.settings = new AdversarialCoverageSettings();

		String settingName;
		// Read all settings
		while (input.hasNext()) {
			settingName = input.next();
			if (!input.next().equals("=")) {
				System.err.println("Bad format for setting: " + settingName);
			}
			String value = input.nextLine();
			if (settingName.equals("grid_danger_values")) {
				// Special case when loading a saved grid

			} else {
				if (!settings.hasProperty(settingName)) {
					settings.setStringProperty(settingName, value);
				} else if (settings.getSettingType(settingName) == AdversarialCoverageSettings.SettingType.INT) {
					settings.setIntProperty(settingName, Integer.parseInt(value));
				} else {
					settings.setStringProperty(settingName, value);
				}
			}
		}

		input.close();
	}

	public void runCoverage() {
		new Timer(settings.getIntProperty("autorun.sleeptime"), new ActionListener() {
			public void actionPerformed(ActionEvent ev) {
				if (env.isFinished() || !AdversarialCoverage.this.isRunning) {
					((javax.swing.Timer) ev.getSource()).stop();
					if (env.isCovered()) {
						System.out.printf("Covered the environment in %d steps.\n", env.getStepCount());
					} else if (env.isFinished()) {
						System.out.println("All robots are broken and cannot continue");
					}
				}
				env.step();
				mainPanel.repaint();
			}
		}).start();
		/*
		 * while (!env.isFinished() && this.isRunning) { env.step();
		 * mainPanel.repaint(); try { Thread.sleep(settings.AUTORUN_SLEEP_TIME);
		 * } catch (InterruptedException e) { e.printStackTrace(); } }
		 */

	}

	public static void main(String[] args) {
		new AdversarialCoverage();
	}
}

/**
 * Container for all the settings needed to configure the coverage simulation.
 * 
 * @author Mike D'Arcy
 *
 */
class AdversarialCoverageSettings {
	final private int DEFAULT_GRID_WIDTH = 10;
	final private int DEFAULT_GRID_HEIGHT = 10;
	final private int DEFAULT_AUTORUN_SLEEP_TIME = 100;

	private Map<String, String> settingsMap = new HashMap<String, String>();
	private Map<String, SettingType> settingTypes = new HashMap<String, SettingType>();
	/**
	 * Error information that can be used to check whether an operation failed
	 */
	Error lastError = Error.NO_ERROR;

	/**
	 * Creates a new {@code AdversarialCoverageSettings} with the default
	 * settings set
	 */
	public AdversarialCoverageSettings() {
		// Set up defaults
		this.setIntProperty("env.grid.width", this.DEFAULT_GRID_WIDTH);
		this.setIntProperty("env.grid.height", this.DEFAULT_GRID_HEIGHT);
		this.setIntProperty("autorun.sleeptime", this.DEFAULT_AUTORUN_SLEEP_TIME);
		this.setStringProperty("grid.dangervalues", "");
	}

	/**
	 * Returns the last error code. Error codes are as
	 * 
	 * @return an error code (int)
	 */
	public Error getLastError() {
		return lastError;
	}

	/**
	 * Resets the last error code to 0.
	 */
	public void resetLastError() {
		this.lastError = Error.NO_ERROR;
	}

	public SettingType getSettingType(String key) {
		if (!this.hasProperty(key)) {
			this.lastError = Error.NO_SUCH_PROPERTY;
			return null;
		} else {
			return this.settingTypes.get(key);
		}
	}

	/**
	 * Gets an integer property
	 * 
	 * @param key
	 *            the name of the integer property
	 * @return the property (int)
	 */
	public int getIntProperty(String key) {
		if (key == null) {
			this.lastError = Error.NULL_KEY;
			return 0;
		} else if (!this.hasProperty(key)) {
			this.lastError = Error.NO_SUCH_PROPERTY;
			return 0;
		} else if (this.settingTypes.get(key) != SettingType.INT) {
			this.lastError = Error.WRONG_SETTING_TYPE;
			return 0;
		}
		int value;
		value = Integer.parseInt(settingsMap.get(key));
		return value;
	}

	/**
	 * Sets an integer property
	 * 
	 * @param key
	 *            the name of the property
	 * @param value
	 *            the value to set
	 */
	public void setIntProperty(String key, int value) {
		if (key == null) {
			return;
		}
		this.settingsMap.put(key, (new Integer(value)).toString());
		this.settingTypes.put(key, SettingType.INT);
	}

	public boolean hasProperty(String key) {
		return settingsMap.containsKey(key);
	}

	public String getStringProperty(String key) {
		if (key == null) {
			this.lastError = Error.NULL_KEY;
			return null;
		} else if (!this.hasProperty(key)) {
			this.lastError = Error.NO_SUCH_PROPERTY;
			return null;
		} else if (this.settingTypes.get(key) != SettingType.STRING) {
			this.lastError = Error.WRONG_SETTING_TYPE;
			return null;
		} else {
			return settingsMap.get(key);
		}
	}

	public void setStringProperty(String key, String value) {
		if (key == null) {
			this.lastError = Error.NULL_KEY;
			return;
		}
		this.settingsMap.put(key, value);
		this.settingTypes.put(key, SettingType.STRING);
	}

	/**
	 * Errors that can occur when performing operations on settings.
	 * <li>{@link #NO_ERROR}</li>
	 * <li>{@link #NULL_KEY}</li>
	 * <li>{@link #NO_SUCH_PROPERTY}</li>
	 * 
	 * @author Mike D'Arcy
	 *
	 */
	enum Error {
		/**
		 * This code means there is no error.
		 */
		NO_ERROR,
		/**
		 * A null key was passed to a function.
		 */
		NULL_KEY,
		/**
		 * A key that did not reference any valid property name was passed to a
		 * function
		 */
		NO_SUCH_PROPERTY,
		/**
		 * The type of the setting did not match the expected setting type
		 */
		WRONG_SETTING_TYPE
	}

	/**
	 * Types of settings
	 * 
	 * @author Mike D'Arcy
	 *
	 */
	enum SettingType {
		INT, STRING
	}
}
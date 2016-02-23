import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.text.NumberFormat;
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

		settings.setIntProperty("autorun.stepdelay", 50);

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
		mainPanel.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				openGridNodeEditDialog(env.getGridNode(mainPanel.getGridX(e.getX()),
						mainPanel.getGridY(e.getY())));
			}
		});
		cw.add(mainPanel);
		JMenuBar menuBar = new JMenuBar();

		// File menu
		JMenu fileMenu = new JMenu("File");
		final JMenuItem exportSettingsMenuItem = new JMenuItem("Export...");
		exportSettingsMenuItem.setToolTipText("Load program settings from a file");
		exportSettingsMenuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent ev) {
				JFileChooser chooser = new JFileChooser();
				int status = chooser.showOpenDialog(cw);
				if (status == JFileChooser.APPROVE_OPTION) {
					File settingsFile = chooser.getSelectedFile();
					exportSettingsToFile(settingsFile);
				}
			}
		});
		fileMenu.add(exportSettingsMenuItem);

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

		final JMenuItem settingsDialogMenuItem = new JMenuItem("Settings...");
		settingsDialogMenuItem.setToolTipText("Open the settings editor dialog");
		settingsDialogMenuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent ev) {
				settings.openSettingsDialog(cw);
			}
		});
		fileMenu.add(settingsDialogMenuItem);

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
		restartCoverageMenuItem.setAccelerator(KeyStroke.getKeyStroke("control R"));
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


	protected void openGridNodeEditDialog(final GridNode gridNode) {
		final JDialog dialog = new JDialog(cw, "Edit node (" + gridNode.getX() + ", " + gridNode.getY() + ")");
		dialog.setModal(true);
		final JPanel contentPanel = new JPanel(new GridLayout(0, 1));
		NumberFormat doubleFormat = NumberFormat.getNumberInstance();
		doubleFormat.setMinimumFractionDigits(4);

		final JPanel settingsPanel = new JPanel(new GridLayout(0, 2));

		final JLabel costLabel = new JLabel("Cost: ");
		settingsPanel.add(costLabel);
		final JFormattedTextField costField = new JFormattedTextField(doubleFormat);
		costField.setValue(new Double(gridNode.getCost()));
		settingsPanel.add(costField);

		final JLabel dangerLabel = new JLabel("Danger: ");
		settingsPanel.add(dangerLabel);
		final JFormattedTextField dangerField = new JFormattedTextField(doubleFormat);
		dangerField.setValue(new Double(gridNode.getDangerProb()));
		settingsPanel.add(dangerField);

		final JLabel coverCountLabel = new JLabel("Times covered: ");
		settingsPanel.add(coverCountLabel);
		final JSpinner coverCountSpinner = new JSpinner();
		coverCountSpinner.setValue(new Integer(gridNode.getCoverCount()));
		settingsPanel.add(coverCountSpinner);

		final JLabel typeLabel = new JLabel("Type: ");
		settingsPanel.add(typeLabel);
		class ComboBoxNodeType {
			NodeType nodetype;
			String label;


			ComboBoxNodeType(NodeType nodetype, String label) {
				this.nodetype = nodetype;
				this.label = label;
			}


			@Override
			public String toString() {
				return label;
			}
		}
		final JComboBox<ComboBoxNodeType> typeBox = new JComboBox<ComboBoxNodeType>();
		typeBox.addItem(new ComboBoxNodeType(NodeType.FREE, "Free"));
		typeBox.addItem(new ComboBoxNodeType(NodeType.OBSTACLE, "Obstacle"));
		for (int i = 0; i < typeBox.getItemCount(); i++) {
			if (gridNode.getNodeType() == typeBox.getItemAt(i).nodetype) {
				typeBox.setSelectedIndex(i);
			}
		}
		settingsPanel.add(typeBox);

		contentPanel.add(settingsPanel);

		JPanel buttonPanel = new JPanel();

		JButton okButton = new JButton("OK");
		okButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				gridNode.setDangerProb(((Number) dangerField.getValue()).doubleValue());
				gridNode.setCost(((Number) costField.getValue()).doubleValue());
				gridNode.setCoverCount(((Number) coverCountSpinner.getValue()).intValue());
				gridNode.setNodeType(((ComboBoxNodeType) typeBox.getSelectedItem()).nodetype);
				mainPanel.repaint();
				dialog.dispose();
			}
		});
		buttonPanel.add(okButton);

		JButton cancelButton = new JButton("Cancel");
		cancelButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				dialog.dispose();
			}
		});
		buttonPanel.add(cancelButton);

		contentPanel.add(buttonPanel);

		dialog.add(contentPanel);
		dialog.pack();
		dialog.setVisible(true);
	}


	/**
	 * Sets up the environment using the settings
	 */
	public void resetCoverageEnvironment() {
		this.env = new GridEnvironment(new Dimension(settings.getIntProperty("env.grid.width"),
				settings.getIntProperty("env.grid.height")));

		// Set up the coverage environment
		if (!this.settings.hasProperty("env.grid.dangervalues")
				|| this.settings.getStringProperty("env.grid.dangervalues").isEmpty()) {
			randomizeDangerLevels();
			randomizeObstacles();
		} else {
			boolean hasAllValues = true;
			Scanner valuesScanner = new Scanner(this.settings.getStringProperty("env.grid.dangervalues"));
			for (int x = 0; x < env.getWidth(); x++) {
				for (int y = 0; y < env.getHeight(); y++) {
					if (valuesScanner.hasNext()) {
						double value = valuesScanner.nextDouble();
						if (0 <= value) {
							this.env.getGridNode(x, y).setDangerProb(value);
						} else {
							this.env.getGridNode(x, y).setNodeType(NodeType.OBSTACLE);
						}
					} else {
						System.err.println(
								"Missing values in grid.dangervalues setting. Switching to randomized danger levels.");
						hasAllValues = false;
						break;
					}
				}
			}
			valuesScanner.close();

			if (!hasAllValues) {
				randomizeDangerLevels();
				randomizeObstacles();
			}
		}

		// Set up the robots
		for (int i = 0; i < 1; i++) {
			GridRobot robot = new GridRobot(i, (int) (Math.random() * env.getWidth()),
					(int) (Math.random() * env.getHeight()));
			GridSensor sensor = new GridSensor(env, robot);
			GridActuator actuator = new GridActuator(env, robot);
			CoverageAlgorithm algo = new GSACGridCoverage(sensor, actuator);//new RandomGridCoverage(sensor, actuator);
			robot.coverAlgo = algo;
			env.addRobot(robot);
		}

		env.init();

		if (mainPanel != null) {
			mainPanel.setEnvironment(this.env);
		}

	}


	private void randomizeObstacles() {
		for (int x = 0; x < env.getWidth(); x++) {
			for (int y = 0; y < env.getHeight(); y++) {
				if (Math.random() < 0.05) {
					env.getGridNode(x, y).setNodeType(NodeType.OBSTACLE);
				}
			}
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
					env.getGridNode(x, y).setDangerProb(0.5); // (env.getGridNode(x, y).getDangerProb() - 0.5) / 10.0
				}
			}
		}
	}


	/**
	 * Loads settings from the given file.
	 * 
	 * @param file
	 *                the file containing setting information
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
			String equalSign = input.next().trim();
			if (!equalSign.equals("=")) {
				System.err.println("Bad format for setting: " + settingName + " (Expected '=', found "
						+ equalSign + ")");
				continue;
			}
			String value = input.nextLine().trim();
			if (settingName.equals("grid_danger_values")) {
				// Special case when loading a saved grid

			} else {
				if (!settings.hasProperty(settingName)) {
					settings.setStringProperty(settingName, value);
				} else if (settings.getSettingType(
						settingName) == AdversarialCoverageSettings.SettingType.INT) {
					settings.setIntProperty(settingName, Integer.parseInt(value));
				} else {
					settings.setStringProperty(settingName, value);
				}
			}
		}

		input.close();
	}


	/**
	 * Exports settings to the given file, so they can be loaded later
	 * 
	 * @param settingsFile
	 *                the file to export to
	 */
	public void exportSettingsToFile(File settingsFile) {
		try {
			PrintWriter pw = new PrintWriter(settingsFile);
			System.out.println("Set: "+this.settings.exportToString());
			pw.println(this.settings.exportToString());
			pw.flush();
			pw.close();
		} catch (FileNotFoundException e) {
			System.err.println("Failed to write settings to file. File not found.");
			e.printStackTrace();
		}
	}


	public void runCoverage() {
		new Thread() {
			public void run() {
				coverageLoop();
			}
		}.start();
	}


	public void coverageLoop() {
		long delay = settings.getIntProperty("autorun.stepdelay");
		while (isRunning) {
			long time = System.currentTimeMillis();
			step();
			mainPanel.repaint();
			if (env.isFinished() || !AdversarialCoverage.this.isRunning) {
				AdversarialCoverage.this.isRunning = false;
				if (env.isCovered()) {
					System.out.printf("Covered the environment in %d steps.\n", env.getStepCount());
				} else if (env.isFinished()) {
					System.out.println("All robots are broken and cannot continue");
				}
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


	private void step() {
		env.step();
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
	final private int DEFAULT_AUTORUN_STEP_DELAY = 100;
	final private int DEFAULT_AUTORUN_FRAME_DELAY = 100;
	final private int DEFAULT_NUM_ROBOTS = 2;

	final private String BOOLEAN_TRUE_STRING = "true";
	final private String BOOLEAN_FALSE_STRING = "false";

	private Map<String, String> settingsMap = new HashMap<String, String>();
	private Map<String, SettingType> settingTypes = new HashMap<String, SettingType>();
	/**
	 * Error information that can be used to check whether an operation
	 * failed
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
		this.setIntProperty("autorun.stepdelay", this.DEFAULT_AUTORUN_STEP_DELAY);
		// this.setIntProperty("autorun.framedelay",
		// this.DEFAULT_AUTORUN_FRAME_DELAY);
		this.setIntProperty("robots.count", this.DEFAULT_NUM_ROBOTS);
		this.setIntProperty("robots.id_0.startpos.x", 0);
		this.setIntProperty("robots.id_0.startpos.y", 0);
		this.setIntProperty("robots.id_1.startpos.x", 5);
		this.setIntProperty("robots.id_1.startpos.y", 5);
		this.setStringProperty("env.grid.dangervalues", "");
		this.setStringProperty("rules.robots.robotsAreObstacles", "true");
	}


	public void openSettingsDialog(Frame parent) {
		System.out.println("Opening settings dialog...");
		final JDialog sd = new JDialog(parent, "Settings");
		sd.setLayout(new BorderLayout());
		sd.setModal(true);

		final JPanel jp = new JPanel();
		jp.setPreferredSize(new Dimension(600, 24 * this.settingsMap.keySet().size()));
		jp.setLayout(new GridLayout(0, 2));

		final Map<String, JTextField> textfields = new HashMap<String, JTextField>();

		for (String settingName : this.settingsMap.keySet()) {
			JLabel settingLabel = new JLabel(settingName);
			textfields.put(settingName, new JTextField(this.settingsMap.get(settingName)));
			jp.add(settingLabel);
			jp.add(textfields.get(settingName));
		}

		final JScrollPane sp = new JScrollPane(jp);
		sd.add(sp);
		JPanel buttonPanel = new JPanel();
		JButton okButton = new JButton("OK");
		okButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent ev) {
				for (String settingName : textfields.keySet()) {
					SettingType type = settingTypes.get(settingName);
					String newValStr = textfields.get(settingName).getText();
					if (type == SettingType.STRING) {
						settingsMap.put(settingName, newValStr);
					} else if (type == SettingType.INT) {
						try {
							int value = Integer.parseInt(newValStr);
							setIntProperty(settingName, value);
						} catch (NumberFormatException e) {
							System.err.println("Failed to parse value for integer setting: "
									+ settingName);
						}
					} else if (type == SettingType.BOOLEAN) {
						if (newValStr.equalsIgnoreCase("true")) {
							setBooleanProperty(settingName, true);
						} else if (newValStr.equalsIgnoreCase("false")) {
							setBooleanProperty(settingName, false);
						} else {
							System.err.println("Failed to parse value for boolean setting: "
									+ settingName);
						}
					}
				}
				sd.dispose();
			}
		});
		buttonPanel.add(okButton);
		JButton cancelButton = new JButton("Cancel");
		cancelButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent ev) {
				sd.dispose();
			}
		});
		buttonPanel.add(cancelButton);
		sd.add(buttonPanel, BorderLayout.SOUTH);
		sd.pack();
		sd.setVisible(true);
	}


	/**
	 * Convert the settings into string form, with one setting per line, in
	 * the format:
	 * 
	 * <pre>
	 * settingName = settingValue
	 * </pre>
	 * 
	 */
	public String exportToString() {
		StringBuilder exportStr = new StringBuilder("");
		for (String s : this.settingsMap.keySet()) {
			exportStr.append(s);
			exportStr.append(" = ");
			exportStr.append(this.settingsMap.get(s));
			exportStr.append('\n');
		}
		return exportStr.toString();
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
	 *                the name of the integer property
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
	 *                the name of the property
	 * @param value
	 *                the value to set
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
	 * Get a boolean property. If an error is encountered, the method
	 * returns false and sets the lastError appropriately.
	 * 
	 * @param key
	 *                the property name
	 * @return the boolean value of the property
	 */
	public boolean getBooleanProperty(String key) {
		if (key == null) {
			this.lastError = Error.NULL_KEY;
			return false;
		} else if (!this.hasProperty(key)) {
			this.lastError = Error.NO_SUCH_PROPERTY;
			return false;
		} else if (this.settingTypes.get(key) != SettingType.BOOLEAN) {
			this.lastError = Error.WRONG_SETTING_TYPE;
			return false;
		}

		String strVal = this.settingsMap.get(key);
		if (strVal.equals("true")) {
			return true;
		} else if (strVal.equals("false")) {
			return false;
		} else {
			this.lastError = Error.BAD_FORMAT;
			return false;
		}

	}


	public void setBooleanProperty(String key, boolean value) {
		if (key == null) {
			this.lastError = Error.NULL_KEY;
			return;
		}

		this.settingTypes.put(key, SettingType.BOOLEAN);
		this.settingsMap.put(key, value ? BOOLEAN_TRUE_STRING : BOOLEAN_FALSE_STRING);
	}

	/**
	 * Errors that can occur when performing operations on settings.
	 * <li>{@link #NO_ERROR}</li>
	 * <li>{@link #NULL_KEY}</li>
	 * <li>{@link #NO_SUCH_PROPERTY}</li>
	 * <li>{@link #WRONG_SETTING_TYPE}</li>
	 * <li>{@link #BAD_FORMAT}</li>
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
		 * A key that did not reference any valid property name was
		 * passed to a function
		 */
		NO_SUCH_PROPERTY,
		/**
		 * The type of the setting did not match the expected setting
		 * type
		 */
		WRONG_SETTING_TYPE,
		/**
		 * The correct setting type could not be produced from the
		 * stored String value
		 */
		BAD_FORMAT
	}

	/**
	 * Types of settings
	 * 
	 * @author Mike D'Arcy
	 *
	 */
	enum SettingType {
		INT, STRING, BOOLEAN
	}
}

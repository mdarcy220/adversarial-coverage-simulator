package adversarialcoverage;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

/**
 * Container for all the settings needed to configure the coverage simulation.
 * 
 * @author Mike D'Arcy
 *
 */
public class AdversarialCoverageSettings {
	final private int DEFAULT_GRID_WIDTH = 5;
	final private int DEFAULT_GRID_HEIGHT = 5;
	final private int DEFAULT_AUTORUN_STEP_DELAY = 0;
	final private int DEFAULT_NUM_ROBOTS = 1;

	final private String BOOLEAN_TRUE_STRING = "true";
	final private String BOOLEAN_FALSE_STRING = "false";

	private Map<String, String> settingsMap = new HashMap<>();
	private Map<String, SettingType> settingTypes = new HashMap<>();
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
		this.setDefaults();

		if (AdversarialCoverage.args.USE_SETTINGS_FILE) {
			this.loadFromFile(new File(AdversarialCoverage.args.SETTINGS_FILE));
		}
	}


	/**
	 * Sets all settings to the default values
	 */
	public void setDefaults() {
		this.setIntProperty("autorun.stepdelay", this.DEFAULT_AUTORUN_STEP_DELAY);
		this.setIntProperty("autorun.max_steps_per_run", Integer.MAX_VALUE-1);
		this.setIntProperty("deepql.history_max", 100000);
		this.setIntProperty("deepql.minibatch_size", 2);
		this.setIntProperty("deepql.nn_input.vision_radius", 5);
		this.setIntProperty("env.grid.height", this.DEFAULT_GRID_HEIGHT);
		this.setIntProperty("env.grid.width", this.DEFAULT_GRID_WIDTH);
		this.setIntProperty("robots.count", this.DEFAULT_NUM_ROBOTS);
		this.setIntProperty("neuralnet.hidden_layer_size", 40);
		this.setIntProperty("neuralnet.num_hidden_layers", 2);
		this.setIntProperty("stats.multirun.batch_size", 100);

		this.setBooleanProperty("autorun.do_repaint", false);
		this.setBooleanProperty("autorun.finished.newgrid", true);
		this.setBooleanProperty("autorun.finished.display_full_stats", false);
		this.setBooleanProperty("autorun.randomize_robot_start", true);
		this.setBooleanProperty("deepql.display.print_q_values", false);
		this.setBooleanProperty("deepql.nn_input.obstacle_layer", true);
		this.setBooleanProperty("display.show_binary_coverage", false);
		this.setBooleanProperty("robots.breakable", true);
		this.setBooleanProperty("rules.robots.robotsAreObstacles", true);
		this.setBooleanProperty("neuralnet.give_global_pos_and_size", true);

		this.setDoubleProperty("deepql.discountfactor", 0.9);
		this.setDoubleProperty("deepql.greedy_epsilon_decrement", 0.0000005);
		this.setDoubleProperty("deepql.greedy_epsilon_minimum", 0.1);
		this.setDoubleProperty("deepql.greedy_epsilon_start", 1.0);
		this.setDoubleProperty("deepql.learning_rate_decay_factor", 1.0);
		this.setDoubleProperty("deepql.reward.cover_again", -0.1);
		this.setDoubleProperty("deepql.reward.cover_unique", 1.0);
		this.setDoubleProperty("deepql.reward.death", -2.0);
		this.setDoubleProperty("deepql.reward.full_coverage", 4.0);
		this.setDoubleProperty("neuralnet.learning_rate", 0.1);
		this.setDoubleProperty("neuralnet.momentum", 0.9);
		this.setDoubleProperty("neuralnet.rms.decay_rate", 0.9);

		this.setStringProperty("deepql.external_torch_nn.io_file_prefix", "/home/ai03/prog/lua/scratch/");
		this.setStringProperty("deepql.nn_setup_mode", "torch");
		this.setStringProperty("env.grid.dangervalues", "@o 0.00 @d 0.3 @r 0.00 0.25");
		this.setStringProperty("neuralnet.loadfile", "");
		this.setStringProperty("neuralnet.trainingtype", "rmsprop");
	}


	/**
	 * Exports settings to the given file, so they can be loaded later
	 * 
	 * @param settingsFile
	 *                the file to export to
	 * @throws FileNotFoundException
	 */
	public void exportToFile(File file) throws FileNotFoundException {
		PrintWriter pw = new PrintWriter(file);
		System.out.println("Set: " + this.exportToString());
		pw.println(this.exportToString());
		pw.flush();
		pw.close();
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


	/**
	 * Gets an integer property
	 * 
	 * @param key
	 *                the name of the integer property
	 * @return the property (int)
	 */
	public double getDoubleProperty(String key) {
		if (key == null) {
			this.lastError = Error.NULL_KEY;
			return 0;
		} else if (!this.hasProperty(key)) {
			this.lastError = Error.NO_SUCH_PROPERTY;
			return 0;
		} else if (this.settingTypes.get(key) != SettingType.DOUBLE) {
			this.lastError = Error.WRONG_SETTING_TYPE;
			return 0;
		}
		double value;
		value = Double.parseDouble(this.settingsMap.get(key));
		return value;
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
		value = Integer.parseInt(this.settingsMap.get(key));
		return value;
	}


	/**
	 * Returns the last error code. Error codes are as
	 * 
	 * @return an error code (int)
	 */
	public Error getLastError() {
		return this.lastError;
	}


	public SettingType getSettingType(String key) {
		if (!this.hasProperty(key)) {
			this.lastError = Error.NO_SUCH_PROPERTY;
			return null;
		}
		return this.settingTypes.get(key);
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
			return this.settingsMap.get(key);
		}
	}


	public boolean hasProperty(String key) {
		return this.settingsMap.containsKey(key);
	}


	/**
	 * Loads settings from the given file
	 * 
	 * @param file
	 *                the file to load settings from
	 */
	public void loadFromFile(File file) {
		Scanner input = null;
		try {
			input = new Scanner(file);
		} catch (FileNotFoundException e) {
			System.err.println("Settings file not found.");
			return;
		}

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

			if (!this.hasProperty(settingName)) {
				this.setStringProperty(settingName, value);
			} else if (this.getSettingType(settingName) == AdversarialCoverageSettings.SettingType.INT) {
				this.setIntProperty(settingName, Integer.parseInt(value));
			} else if (this.getSettingType(settingName) == AdversarialCoverageSettings.SettingType.DOUBLE) {
				this.setDoubleProperty(settingName, Double.parseDouble(value));
			} else if (this.getSettingType(
					settingName) == AdversarialCoverageSettings.SettingType.BOOLEAN) {
				if (value.equalsIgnoreCase("true")) {
					this.setBooleanProperty(settingName, true);
				} else if (value.equalsIgnoreCase("false")) {
					this.setBooleanProperty(settingName, false);
				}
			} else {
				this.setStringProperty(settingName, value);
			}

		}

		input.close();
	}


	public void openSettingsDialog(Frame parent) {
		System.out.println("Opening settings dialog...");
		final JDialog sd = new JDialog(parent, "Settings");
		sd.setLayout(new BorderLayout());
		sd.setModal(true);

		final JPanel jp = new JPanel();
		jp.setPreferredSize(new Dimension(600, 24 * this.settingsMap.keySet().size()));
		jp.setLayout(new GridLayout(0, 2));

		final Map<String, JTextField> textfields = new HashMap<>();

		String[] settingNames = new String[this.settingsMap.keySet().size()];
		this.settingsMap.keySet().toArray(settingNames);
		Arrays.sort(settingNames);

		for (String settingName : settingNames) {
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
					SettingType type = AdversarialCoverageSettings.this.settingTypes
							.get(settingName);
					String newValStr = textfields.get(settingName).getText();
					if (type == SettingType.STRING) {
						AdversarialCoverageSettings.this.settingsMap.put(settingName,
								newValStr);
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
					} else if (type == SettingType.DOUBLE) {
						try {
							double value = Double.parseDouble(newValStr);
							setDoubleProperty(settingName, value);
						} catch (NumberFormatException e) {
							System.err.println("Failed to parse value for integer setting: "
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
	 * Resets the last error code to 0.
	 */
	public void resetLastError() {
		this.lastError = Error.NO_ERROR;
	}


	public void setBooleanProperty(String key, boolean value) {
		if (key == null) {
			this.lastError = Error.NULL_KEY;
			return;
		}

		this.settingTypes.put(key, SettingType.BOOLEAN);
		this.settingsMap.put(key, value ? this.BOOLEAN_TRUE_STRING : this.BOOLEAN_FALSE_STRING);
	}


	/**
	 * Sets an integer property
	 * 
	 * @param key
	 *                the name of the property
	 * @param value
	 *                the value to set
	 */
	public void setDoubleProperty(String key, double value) {
		if (key == null) {
			return;
		}
		this.settingsMap.put(key, (new Double(value)).toString());
		this.settingTypes.put(key, SettingType.DOUBLE);
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
		INT, DOUBLE, STRING, BOOLEAN
	}
}

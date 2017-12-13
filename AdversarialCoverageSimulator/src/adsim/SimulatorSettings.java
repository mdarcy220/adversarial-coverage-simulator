package adsim;

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
public class SimulatorSettings {

	final private String BOOLEAN_TRUE_STRING = "true";
	final private String BOOLEAN_FALSE_STRING = "false";

	private Map<String, String> settingsMap = new HashMap<>();
	private Map<String, SettingType> settingTypes = new HashMap<>();
	/**
	 * Error information that can be used to check whether an operation failed
	 */
	Error lastError = Error.NO_ERROR;


	/**
	 * Creates a new {@code SimulatorSettings} with the default settings set
	 */
	public SimulatorSettings() {
		this.registerConsoleCommands();
		// Set up defaults
		this.setDefaults();

		if (SimulatorMain.args.USE_SETTINGS_FILE) {
			this.loadFromFile(new File(SimulatorMain.args.SETTINGS_FILE));
		}
	}


	private void registerConsoleCommands() {

		ConsoleController controller = SimulatorMain.controller;
		controller.registerCommand(":set", new TerminalCommand() {
			@Override
			public void execute(String[] args) {
				if (args.length < 2) {
					return;
				}
				setAuto(args[0], args[1]);
				SimulatorMain.getEngine().reloadSettings();
			}
		});

		controller.registerCommand(":get", new TerminalCommand() {
			@Override
			public void execute(String[] args) {
				if (args.length < 1) {
					return;
				}
				System.out.print(getAsString(args[0]));
			}
		});


		controller.registerCommand(":showsettings", new TerminalCommand() {
			@Override
			public void execute(String[] args) {
				if (0 < args.length && args[0].equals("ascommands")) {
					System.out.println(exportToCommandString());
				} else {
					System.out.println(exportToString());
				}
			}
		});
	}


	/**
	 * Sets all settings to the default values
	 */
	public void setDefaults() {
		this.setInt("autorun.max_steps_per_run", Integer.MAX_VALUE - 1);
		this.setInt("autorun.stepdelay", 0);
		this.setInt("deepql.history_max", 1);
		this.setInt("deepql.minibatch_size", 0);
		this.setInt("deepql.minibatch_interval", 1);
		this.setInt("deepql.minibatch_seq.fullep.numCodes", -1);
		this.setInt("deepql.external.rnn.num_codes_per_minibatch", 1);
		this.setInt("deepql.nn_input.vision_radius", 5);
		this.setInt("env.grid.height", 5);
		this.setInt("env.grid.maxheight", 5);
		this.setInt("env.grid.maxwidth", 5);
		this.setInt("env.grid.minheight", 5);
		this.setInt("env.grid.minwidth", 5);
		this.setInt("env.grid.width", 5);
		this.setInt("logging.deepql.loss_sampling_interval", 500);
		this.setInt("logging.deepql.loss_display_interval", 500);
		this.setInt("neuralnet.hidden_layer_size", 30);
		this.setInt("neuralnet.num_hidden_layers", 2);
		this.setInt("robots.count", 1);
		this.setInt("stats.multirun.batch_size", 100);

		this.setBoolean("autorun.do_repaint", false);
		this.setBoolean("autorun.finished.newgrid", true);
		this.setBoolean("autorun.finished.display_full_stats", false);
		this.setBoolean("autorun.randomize_robot_start", true);
		this.setBoolean("deepql.always_forward_nninput", false);
		this.setBoolean("deepql.display.print_q_values", false);
		this.setBoolean("deepql.external.use_fast_forwards", false);
		this.setBoolean("deepql.nn_input.obstacle_layer", true);
		this.setBoolean("deepql.use_external_qlearner", true);
		this.setBoolean("display.show_binary_coverage", false);
		this.setBoolean("deepql.statepreprocessor.attempt_normalization", true);
		this.setBoolean("env.clear_adjacent_cells_on_init", false);
		this.setBoolean("env.grid.force_square", true);
		this.setBoolean("env.variable_grid_size", false);
		this.setBoolean("neuralnet.give_global_pos_and_size", false);
		this.setBoolean("neuralnet.torch.use_partial_transitions", false);
		this.setBoolean("robots.breakable", true);
		this.setBoolean("rules.robots.robotsAreObstacles", true);

		this.setDouble("deepql.discountfactor", 0.9);
		this.setDouble("deepql.greedy_epsilon_decrement", 5E-7);
		this.setDouble("deepql.greedy_epsilon_minimum", 0.1);
		this.setDouble("deepql.greedy_epsilon_start", 1.0);
		this.setDouble("deepql.learning_rate_decay_factor", 1.0);
		this.setDouble("deepql.reward.cover_again", -0.01);
		this.setDouble("deepql.reward.cover_unique", 0.1);
		this.setDouble("deepql.reward.death", -0.2);
		this.setDouble("deepql.reward.full_coverage", 0.4);
		this.setDouble("deepql.statepreprocessor.out_of_bounds_vals.danger", 0.0);
		this.setDouble("deepql.statepreprocessor.out_of_bounds_vals.cover", 0.0);
		this.setDouble("deepql.statepreprocessor.out_of_bounds_vals.obstacle", 1.0);
		this.setDouble("neuralnet.learning_rate", 0.1);
		this.setDouble("neuralnet.momentum", 0.9);
		this.setDouble("neuralnet.rms.decay_rate", 0.9);

		this.setString("adsim.algorithm_name", "GSACGC");
		this.setString("deepql.external_torch_nn.io_file_prefix", "/home/ai04/midarcy/prog/lua/scratch/environments/betatester/");
		this.setString("deepql.external_torch_nn.nninput_file_name", "input2.dat");
		this.setString("deepql.external_torch_nn.nnoutput_file_name", "output2.dat");
		this.setString("deepql.minibatch_seq_type", "manual");
		this.setString("deepql.nn_setup_mode", "native");
		this.setString("deepql.statepreprocessor.vision_type", "CENTERED_SNAP_TO_EDGES");
		this.setString("env.grid.dangervalues", "@o 0.00 @d 0.3 @r 0.00 0.25");
		this.setString("hooks.env.post_init.cmd", "");
		this.setString("logging.logfile", "");
		this.setString("neuralnet.torch.minibatch_code", "m");
		this.setString("neuralnet.loadfile", "");
		this.setString("neuralnet.trainingtype", "momentum");
	}


	/**
	 * Exports settings to the given file, so they can be loaded later
	 * 
	 * @param settingsFile
	 *                the file to export to
	 * @throws FileNotFoundException
	 */
	public void exportToFile(File file) {
		try (PrintWriter pw = new PrintWriter(file)) {
			System.out.println("Set: " + this.exportToString());
			pw.println(this.exportToString());
			pw.flush();
			pw.close();
		} catch (FileNotFoundException e) {
			System.err.println("Failed to write settings to file. File not found.");
		}
	}


	/**
	 * Convert the settings into string form, with one setting per line, in the
	 * format:
	 * 
	 * <pre>
	 * settingName = settingValue
	 * </pre>
	 * 
	 */
	public String exportToString() {
		StringBuilder exportStr = new StringBuilder("");
		String[] settingNames = getSortedSettingNames();
		for (String s : settingNames) {
			exportStr.append(s);
			exportStr.append(" = ");
			exportStr.append(this.settingsMap.get(s));
			exportStr.append('\n');
		}
		return exportStr.toString();
	}


	public String exportToCommandString() {
		StringBuilder exportStr = new StringBuilder("");
		String[] settingNames = getSortedSettingNames();
		for (String s : settingNames) {
			exportStr.append(":set ");
			exportStr.append(s);
			exportStr.append(" \"");
			exportStr.append(this.settingsMap.get(s));
			exportStr.append("\"\n");
		}
		return exportStr.toString();
	}


	/**
	 * Get a boolean property. If an error is encountered, the method returns false
	 * and sets the lastError appropriately.
	 * 
	 * @param key
	 *                the property name
	 * @return the boolean value of the property
	 */
	public boolean getBoolean(String key) {
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
	public double getDouble(String key) {
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
	public int getInt(String key) {
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


	public String getString(String key) {
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


	/**
	 * Gets a property as its internal string value, even if the property is not of
	 * type string.
	 * 
	 * @param key
	 *                the key for the property
	 * @return the property value as a string
	 */
	public String getAsString(String key) {
		if (key == null) {
			this.lastError = Error.NULL_KEY;
			return null;
		} else if (!this.hasProperty(key)) {
			this.lastError = Error.NO_SUCH_PROPERTY;
			return null;
		}

		return this.settingsMap.get(key);
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
				System.err.println("Bad format for setting: " + settingName + " (Expected '=', found " + equalSign + ")");
				continue;
			}
			String value = input.nextLine().trim();

			if (!this.hasProperty(settingName)) {
				this.setString(settingName, value);
			} else if (this.getSettingType(settingName) == SimulatorSettings.SettingType.INT) {
				this.setInt(settingName, Integer.parseInt(value));
			} else if (this.getSettingType(settingName) == SimulatorSettings.SettingType.DOUBLE) {
				this.setDouble(settingName, Double.parseDouble(value));
			} else if (this.getSettingType(settingName) == SimulatorSettings.SettingType.BOOLEAN) {
				if (value.equalsIgnoreCase("true")) {
					this.setBoolean(settingName, true);
				} else if (value.equalsIgnoreCase("false")) {
					this.setBoolean(settingName, false);
				}
			} else {
				this.setString(settingName, value);
			}

		}

		input.close();
	}


	public String[] getSortedSettingNames() {
		String[] settingNames = new String[this.settingsMap.keySet().size()];
		this.settingsMap.keySet().toArray(settingNames);
		Arrays.sort(settingNames);
		return settingNames;
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

		String[] settingNames = getSortedSettingNames();

		for (String settingName : settingNames) {
			JLabel settingLabel = new JLabel(settingName);
			textfields.put(settingName, new JTextField(this.settingsMap.get(settingName)));
			jp.add(settingLabel);
			jp.add(textfields.get(settingName));
		}

		final JScrollPane sp = new JScrollPane(jp);
		sp.getVerticalScrollBar().setUnitIncrement(16);
		sd.add(sp);
		JPanel buttonPanel = new JPanel();
		JButton okButton = new JButton("OK");
		okButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent ev) {
				for (String settingName : textfields.keySet()) {
					SettingType type = SimulatorSettings.this.settingTypes.get(settingName);
					String newValStr = textfields.get(settingName).getText();
					if (type == SettingType.STRING) {
						SimulatorSettings.this.settingsMap.put(settingName, newValStr);
					} else if (type == SettingType.INT) {
						try {
							int value = Integer.parseInt(newValStr);
							setInt(settingName, value);
						} catch (NumberFormatException e) {
							System.err.println("Failed to parse value for integer setting: " + settingName);
						}
					} else if (type == SettingType.BOOLEAN) {
						if (newValStr.equalsIgnoreCase("true")) {
							setBoolean(settingName, true);
						} else if (newValStr.equalsIgnoreCase("false")) {
							setBoolean(settingName, false);
						} else {
							System.err.println("Failed to parse value for boolean setting: " + settingName);
						}
					} else if (type == SettingType.DOUBLE) {
						try {
							double value = Double.parseDouble(newValStr);
							setDouble(settingName, value);
						} catch (NumberFormatException e) {
							System.err.println("Failed to parse value for integer setting: " + settingName);
						}
					}
				}
				sd.dispose();
				SimulatorMain.getEngine().reloadSettings();
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


	public void setBoolean(String key, boolean value) {
		if (key == null) {
			this.lastError = Error.NULL_KEY;
			return;
		}

		this.settingTypes.put(key, SettingType.BOOLEAN);
		this.settingsMap.put(key, value ? this.BOOLEAN_TRUE_STRING : this.BOOLEAN_FALSE_STRING);
	}


	/**
	 * Sets a boolean property based on the given string. If the value string is
	 * "true", ignoring case, the boolean will be set to true. If it is "false",
	 * ignoring case, the boolean will be false. If it is anything else, no value will
	 * be set and the lastError will be set to <code>Error.BAD_FORMAT</code>.
	 * 
	 * @param key
	 * @param value
	 */
	public void setBoolean(String key, String value) {
		try {
			boolean boolValue = false;
			if (value.equalsIgnoreCase("true")) {
				boolValue = true;
			} else if (value.equalsIgnoreCase("false")) {
				boolValue = false;
			} else {
				this.lastError = Error.BAD_FORMAT;
				return;
			}
			this.setBoolean(key, boolValue);
		} catch (NumberFormatException e) {
			this.lastError = Error.BAD_FORMAT;
		} catch (NullPointerException e) {
			this.lastError = Error.NULL_VALUE;
		}
	}


	/**
	 * Sets a double property
	 * 
	 * @param key
	 *                the name of the property
	 * @param value
	 *                the value to set
	 */
	public void setDouble(String key, double value) {
		if (key == null) {
			this.lastError = Error.NULL_KEY;
			return;
		}
		this.settingsMap.put(key, (new Double(value)).toString());
		this.settingTypes.put(key, SettingType.DOUBLE);
	}


	/**
	 * Sets a double property
	 * 
	 * @param key
	 *                the name of the property
	 * @param value
	 *                the value to set
	 */
	public void setDouble(String key, String value) {
		try {
			this.setDouble(key, Double.parseDouble(value));
		} catch (NumberFormatException e) {
			this.lastError = Error.BAD_FORMAT;
		} catch (NullPointerException e) {
			this.lastError = Error.NULL_VALUE;
		}
	}


	/**
	 * Sets an integer property
	 * 
	 * @param key
	 *                the name of the property
	 * @param value
	 *                the value to set
	 */
	public void setInt(String key, int value) {
		if (key == null) {
			this.lastError = Error.NULL_KEY;
			return;
		}
		this.settingsMap.put(key, (new Integer(value)).toString());
		this.settingTypes.put(key, SettingType.INT);
	}


	/**
	 * Sets an integer property
	 * 
	 * @param key
	 *                the name of the property
	 * @param value
	 *                the value to set
	 */
	public void setInt(String key, String value) {
		try {
			this.setInt(key, Integer.parseInt(value));
		} catch (NumberFormatException e) {
			this.lastError = Error.BAD_FORMAT;
		} catch (NullPointerException e) {
			this.lastError = Error.NULL_VALUE;
		}
	}


	public void setString(String key, String value) {
		if (key == null) {
			this.lastError = Error.NULL_KEY;
			return;
		}
		this.settingsMap.put(key, value);
		this.settingTypes.put(key, SettingType.STRING);
	}


	public void setAuto(String key, String value) {
		if (key == null) {
			this.lastError = Error.NULL_KEY;
			return;
		}
		if (!this.hasProperty(key)) {
			this.setString(key, value);
			return;
		}

		SettingType type = this.getSettingType(key);
		switch (type) {
		case INT:
			this.setInt(key, value);
			break;
		case DOUBLE:
			this.setDouble(key, value);
			break;
		case BOOLEAN:
			this.setBoolean(key, value);
			break;
		case STRING: // FALLTHROUGH
		default:
			this.setString(key, value);
		}
	}

	/**
	 * Errors that can occur when performing operations on settings.
	 * <li>{@link #NO_ERROR}</li>
	 * <li>{@link #NULL_KEY}</li>
	 * <li>{@link #NULL_VALUE}</li>
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
		 * A null value was passed to a function.
		 */
		NULL_VALUE,
		/**
		 * A key that did not reference any valid property name was passed to a
		 * function
		 */
		NO_SUCH_PROPERTY,
		/**
		 * The type of the setting did not match the expected setting type
		 */
		WRONG_SETTING_TYPE,
		/**
		 * The correct setting type could not be produced from the stored String
		 * value
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

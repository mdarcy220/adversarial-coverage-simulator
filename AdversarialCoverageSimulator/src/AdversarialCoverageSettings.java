import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
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
class AdversarialCoverageSettings {
	final private int DEFAULT_GRID_WIDTH = 10;
	final private int DEFAULT_GRID_HEIGHT = 10;
	final private int DEFAULT_AUTORUN_STEP_DELAY = 100;
	// final private int DEFAULT_AUTORUN_FRAME_DELAY = 100;
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

		this.setBooleanProperty("robots.breakable", false);

		this.setStringProperty("env.grid.dangervalues", "@o 0.05 @d 0.5 @r 0.125 0.1825");
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

		final Map<String, JTextField> textfields = new HashMap<>();

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
		return this.lastError;
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
		}
		return this.settingTypes.get(key);
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
		return this.settingsMap.containsKey(key);
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
		this.settingsMap.put(key, value ? this.BOOLEAN_TRUE_STRING : this.BOOLEAN_FALSE_STRING);
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
			if (settingName.equals("grid_danger_values")) {
				// Special case when loading a saved grid

			} else {
				if (!this.hasProperty(settingName)) {
					this.setStringProperty(settingName, value);
				} else if (this.getSettingType(
						settingName) == AdversarialCoverageSettings.SettingType.INT) {
					this.setIntProperty(settingName, Integer.parseInt(value));
				} else {
					this.setStringProperty(settingName, value);
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
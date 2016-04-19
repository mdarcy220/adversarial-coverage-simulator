package adversarialcoverage;

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.text.NumberFormat;
import javax.swing.*;

public class AdversarialCoverage {

	public static AdversarialCoverageSettings settings = new AdversarialCoverageSettings();
	GridEnvironment env = null;
	CoverageWindow cw = new CoverageWindow();
	CoveragePanel mainPanel;
	static SimulationStats stats;

	/**
	 * Keeps track of whether the coverage simulation is running
	 */
	boolean isRunning = false;

	/**
	 * Whether the user has quit the coverage
	 */
	boolean hasQuit = false;


	public AdversarialCoverage() {

		resetCoverageEnvironment();

		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				createAndShowGUI();
			}
		});
	}


	private void createAndShowGUI() {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (UnsupportedLookAndFeelException e) {
			// handle exception
		} catch (ClassNotFoundException e) {
			// handle exception
		} catch (InstantiationException e) {
			// handle exception
		} catch (IllegalAccessException e) {
			// handle exception
		}

		this.mainPanel = new CoveragePanel(this.env);
		// Set up the window
		this.cw.setVisible(true);
		this.mainPanel.setSize(500, 500);
		this.mainPanel.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				openGridNodeEditDialog(AdversarialCoverage.this.env.getGridNode(
						AdversarialCoverage.this.mainPanel.getGridX(e.getX()),
						AdversarialCoverage.this.mainPanel.getGridY(e.getY())));
			}
		});
		this.cw.add(this.mainPanel);
		JMenuBar menuBar = new JMenuBar();

		// File menu
		JMenu fileMenu = new JMenu("File");
		final JMenuItem exportSettingsMenuItem = new JMenuItem("Export...");
		exportSettingsMenuItem.setToolTipText("Load program settings from a file");
		exportSettingsMenuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent ev) {
				JFileChooser chooser = new JFileChooser();
				int status = chooser.showOpenDialog(AdversarialCoverage.this.cw);
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
				int status = chooser.showOpenDialog(AdversarialCoverage.this.cw);
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
				settings.openSettingsDialog(AdversarialCoverage.this.cw);
				AdversarialCoverage.this.env.reloadSettings();
			}
		});
		fileMenu.add(settingsDialogMenuItem);


		final JMenuItem exportMapDialogMenuItem = new JMenuItem("Export grid...");
		exportMapDialogMenuItem.setToolTipText("Export the current grid as a parsable input string");
		exportMapDialogMenuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent ev) {
				System.out.println(AdversarialCoverage.this.env.exportToString());
			}
		});
		fileMenu.add(exportMapDialogMenuItem);

		menuBar.add(fileMenu);

		// Run menu
		JMenu runMenu = new JMenu("Run");
		final JMenuItem runCoverageMenuItem = new JMenuItem("Autorun");
		final JMenuItem pauseCoverageMenuItem = new JMenuItem("Stop");
		final JMenuItem stepCoverageMenuItem = new JMenuItem("Step");
		final JMenuItem restartCoverageMenuItem = new JMenuItem("Restart (keep grid)");
		final JMenuItem newCoverageMenuItem = new JMenuItem("New coverage");

		runCoverageMenuItem.setToolTipText("Automatically step until coverage is done");
		runCoverageMenuItem.setAccelerator(KeyStroke.getKeyStroke("F6"));
		runCoverageMenuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent ev) {
				AdversarialCoverage.this.isRunning = true;
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
				AdversarialCoverage.this.isRunning = false;
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
				if (!AdversarialCoverage.this.env.isFinished()) {
					AdversarialCoverage.this.env.step();
				}
				AdversarialCoverage.this.mainPanel.repaint();
			}
		});
		runMenu.add(stepCoverageMenuItem);

		restartCoverageMenuItem.setToolTipText("Reinitialize the current coverage");
		restartCoverageMenuItem.setAccelerator(KeyStroke.getKeyStroke("control R"));
		restartCoverageMenuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent ev) {
				AdversarialCoverage.this.isRunning = false;
				runCoverageMenuItem.setEnabled(true);
				stepCoverageMenuItem.setEnabled(true);
				pauseCoverageMenuItem.setEnabled(false);
				// resetCoverageEnvironment();
				reinitializeCoverage();
				AdversarialCoverage.this.mainPanel.repaint();
			}
		});
		runMenu.add(restartCoverageMenuItem);

		newCoverageMenuItem.setToolTipText("Start a new coverage");
		newCoverageMenuItem.setAccelerator(KeyStroke.getKeyStroke("control N"));
		newCoverageMenuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent ev) {
				AdversarialCoverage.this.isRunning = false;
				runCoverageMenuItem.setEnabled(true);
				stepCoverageMenuItem.setEnabled(true);
				pauseCoverageMenuItem.setEnabled(false);
				// resetCoverageEnvironment();
				resetCoverageEnvironment();
				AdversarialCoverage.this.mainPanel.repaint();
			}
		});
		runMenu.add(newCoverageMenuItem);

		menuBar.add(runMenu);
		this.cw.setJMenuBar(menuBar);
	}


	protected void openGridNodeEditDialog(final GridNode gridNode) {
		final JDialog dialog = new JDialog(this.cw,
				"Edit node (" + gridNode.getX() + ", " + gridNode.getY() + ")");
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
				return this.label;
			}
		}
		final JComboBox<ComboBoxNodeType> typeBox = new JComboBox<>();
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
				AdversarialCoverage.this.mainPanel.repaint();
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
		genGridFromDangerValuesString(AdversarialCoverage.settings.getStringProperty("env.grid.dangervalues"));
		

		// Set up the robots
		for (int i = 0; i < AdversarialCoverage.settings.getIntProperty("robots.count"); i++) {
			GridRobot robot = new GridRobot(i, (int) (Math.random() * this.env.getWidth()),
					(int) (Math.random() * this.env.getHeight()));
			GridSensor sensor = new GridSensor(this.env, robot);
			GridActuator actuator = new GridActuator(this.env, robot);
			CoverageAlgorithm algo = new DeepQLGridCoverage(sensor, actuator);
			robot.coverAlgo = algo;
			this.env.addRobot(robot);
		}
		AdversarialCoverage.stats = new SimulationStats(this.env, this.env.getRobotList());

		this.env.init();

		if (this.mainPanel != null) {
			this.mainPanel.setEnvironment(this.env);
		}

	}


	/**
	 * Creates a grid using the given danger values string.
	 * 
	 * @param dangerValStr
	 *                the string to be used when generating the grid.
	 */
	private void genGridFromDangerValuesString(String dangerValStr) {
		GridNodeGenerator nodegen = new GridNodeGenerator();
		nodegen.useScanner(dangerValStr);
		for (int x = 0; x < this.env.getWidth(); x++) {
			for (int y = 0; y < this.env.getHeight(); y++) {
				nodegen.genNext(this.env.getGridNode(x, y));
			}
		}
	}


	private void reinitializeCoverage() {
		for (int x = 0; x < this.env.getWidth(); x++) {
			for (int y = 0; y < this.env.getHeight(); y++) {
				this.env.getGridNode(x, y).setCoverCount(0);
			}
		}
		this.env.init();
	}


	/**
	 * Loads settings from the given file.
	 * 
	 * @param file
	 *                the file containing setting information
	 */
	public void loadSettingsFromFile(File file) {
		AdversarialCoverage.settings.loadFromFile(file);
	}


	/**
	 * Exports settings to the given file, so they can be loaded later
	 * 
	 * @param settingsFile
	 *                the file to export to
	 */
	public void exportSettingsToFile(File settingsFile) {
		try {
			AdversarialCoverage.settings.exportToFile(settingsFile);
		} catch (FileNotFoundException e) {
			System.err.println("Failed to write settings to file. File not found.");
			e.printStackTrace();
		}
	}


	public void printStats(PrintStream ps) {
		ps.println("Name\tValue");

		ps.printf("Avg. covers per free cell\t%f\n", AdversarialCoverage.stats.getAvgCoversPerFreeCell());
		ps.printf("Max covers of a cell\t%d\n", AdversarialCoverage.stats.getMaxCellCovers());
		ps.printf("Min covers of a cell\t%d\n", AdversarialCoverage.stats.getMinCellCovers());
		ps.printf("Number of cells covered exactly once\t%d\n",
				AdversarialCoverage.stats.numFreeCellsCoveredNTimes(1));
		ps.printf("Total cells in grid\t%d\n", AdversarialCoverage.stats.getTotalCells());
		ps.printf("Total non-obstacle cells\t%d\n", AdversarialCoverage.stats.getTotalFreeCells());
		ps.printf("Total time steps\t%d\n", AdversarialCoverage.stats.getNumTimeSteps());
		ps.printf("Robots broken\t%d\n", AdversarialCoverage.stats.getNumBrokenRobots());
		ps.printf("Robots surviving\t%d\n", AdversarialCoverage.stats.getNumSurvivingRobots());
		ps.printf("Percent covered\t%f\n", AdversarialCoverage.stats.getFractionCovered() * 100.0);
		ps.printf("Best survivability\t%f\n", AdversarialCoverage.stats.getMaxSurvivability());
		ps.printf("Best whole area coverage probability\t%.6E\n",
				AdversarialCoverage.stats.getMaxCoverageProb());
	}


	public void runCoverage() {
		new Thread() {
			@Override
			public void run() {
				coverageLoop();
			}
		}.start();
	}


	public void coverageLoop() {
		// Update settings
		this.env.reloadSettings();

		long statsBatchSize = AdversarialCoverage.settings.getIntProperty("stats.multirun.batch_size");
		long delay = settings.getIntProperty("autorun.stepdelay");
		boolean doRepaint = settings.getBooleanProperty("autorun.do_repaint");

		while (this.isRunning) {
			long time = System.currentTimeMillis();
			step();
			if (doRepaint) {
				this.mainPanel.repaint();
			}
			if (this.env.isFinished() || !AdversarialCoverage.this.isRunning) {

				if (this.env.isCovered()) {
					System.out.printf("Covered the environment in %d steps.\n",
							this.env.getStepCount());
					AdversarialCoverage.stats.startNewRun();
					if (AdversarialCoverage.stats.getRunsInCurrentBatch() % statsBatchSize == 0) {
						System.out.printf("Average steps for last %d coverages: %f\n",
								AdversarialCoverage.stats.getRunsInCurrentBatch(),
								AdversarialCoverage.stats.getBatchAvgSteps());
						AdversarialCoverage.stats.reset();
					}
				} else if (this.env.isFinished()) {
					// System.out.println("All robots are
					// broken and cannot continue");
				}
				if (this.env.isFinished()) {

					// File settingsFile = new
					// File("/tmp/stats.txt");
					// try {
					// printStats(new
					// PrintStream(settingsFile));
					// } catch (FileNotFoundException e) {
					// e.printStackTrace();
					// }
					//
					if (settings.getBooleanProperty("autorun.finished.display_full_stats")) {
						printStats(new PrintStream(System.out));
					} else {
						// System.out.printf("Max covers
						// of a cell\t%d\n",
						// AdversarialCoverage.stats.getMaxCellCovers());
						// System.out.printf("Total time
						// steps\t%d\n",
						// AdversarialCoverage.stats.getNumTimeSteps());
					}
					if (settings.getBooleanProperty("autorun.finished.newgrid")) {
						// resetCoverageEnvironment();
						// reinitializeCoverage();
						for (GridRobot r : this.env.getRobotList()) {
							r.setBroken(false);
						}

						genGridFromDangerValuesString(AdversarialCoverage.settings
								.getStringProperty("env.grid.dangervalues"));
						this.env.init();

						if (this.mainPanel != null) {
							this.mainPanel.setEnvironment(this.env);
						}
						this.mainPanel.repaint();
						// System.out.println("New
						// environment!");
					} else {
						AdversarialCoverage.this.isRunning = false;
					}
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
		this.env.step();
		stats.updateTimeStep();
	}


	public static void main(String[] args) {
		new AdversarialCoverage();
	}
}

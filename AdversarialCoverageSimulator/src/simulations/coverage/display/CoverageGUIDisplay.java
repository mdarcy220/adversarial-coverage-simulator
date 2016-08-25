package simulations.coverage.display;

import java.awt.GraphicsEnvironment;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.text.NumberFormat;

import javax.swing.*;

import adsim.SimulatorMain;
import gridenv.GridNode;
import gridenv.NodeType;
import simulations.coverage.CoverageSimulation;
import adsim.Display;

public class CoverageGUIDisplay implements Display {
	private CoveragePanel mainPanel = null;
	private JFrame frame = null;
	private CoverageSimulation sim = null;


	private CoverageGUIDisplay(final CoverageSimulation sim) {
		this.sim = sim;
		this.frame = new JFrame("Adversarial Coverage Simulator");
		// initialize the window
		this.frame.setSize(500, 400);
		this.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}


	public static CoverageGUIDisplay createInstance(final CoverageSimulation sim) {
		if (GraphicsEnvironment.isHeadless()) {
			System.err.println("Could not instantiate GUI display because environment is headless.");
			return null;
		}
		CoverageGUIDisplay instance = new CoverageGUIDisplay(sim);
		return instance;
	}


	@Override
	public void dispose() {
		this.frame.setVisible(false);
		this.frame.dispose();
	}


	public void setup() {
		this.setNativeLookAndFeel();

		this.mainPanel = this.createMainPanel();
		this.frame.add(this.mainPanel);

		JMenuBar menuBar = new JMenuBar();
		this.setupFileMenu(menuBar);
		this.setupRunMenu(menuBar);
		this.frame.setJMenuBar(menuBar);

		this.frame.setVisible(true);
	}


	private void setNativeLookAndFeel() {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (UnsupportedLookAndFeelException e) {
			System.err.println("Native look and feel is not supported.");
		} catch (ClassNotFoundException e) {
			System.err.println("Could not find system look and feel class name.");
		} catch (InstantiationException e) {
			System.err.println("Failed to instantiate native look and feel.");
		} catch (IllegalAccessException e) {
			System.err.println("Setting native look and feel triggered IllegalAccessException.");
		}
	}


	private CoveragePanel createMainPanel() {
		final CoveragePanel panel = new CoveragePanel(this.sim);
		panel.setSize(500, 500);
		panel.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				openGridNodeEditDialog(
						CoverageGUIDisplay.this.sim.getEnv().getGridNode(panel.getGridX(e.getX()), panel.getGridY(e.getY())));
			}
		});
		return panel;
	}


	private void setupFileMenu(JMenuBar menuBar) {
		// File menu
		final JMenu fileMenu = new JMenu("File");
		final JMenuItem exportSettingsMenuItem = new JMenuItem("Export...");
		final JMenuItem loadSettingsMenuItem = new JMenuItem("Load settings from file...");
		final JMenuItem settingsDialogMenuItem = new JMenuItem("Settings...");
		final JMenuItem exportMapDialogMenuItem = new JMenuItem("Export grid...");
		final JMenuItem switchToHeadlessMenuItem = new JMenuItem("Go headless");

		exportSettingsMenuItem.setToolTipText("Load program settings from a file");
		exportSettingsMenuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent ev) {
				CoverageGUIDisplay.this.showExportSettingsDialog();
			}
		});
		fileMenu.add(exportSettingsMenuItem);

		loadSettingsMenuItem.setToolTipText("Load program settings from a file");
		loadSettingsMenuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent ev) {
				CoverageGUIDisplay.this.showLoadSettingsDialog();
			}
		});
		fileMenu.add(loadSettingsMenuItem);

		settingsDialogMenuItem.setToolTipText("Open the settings editor dialog");
		settingsDialogMenuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent ev) {
				SimulatorMain.settings.openSettingsDialog(CoverageGUIDisplay.this.frame);
				CoverageGUIDisplay.this.sim.getEnv().reloadSettings();
			}
		});
		fileMenu.add(settingsDialogMenuItem);

		exportMapDialogMenuItem.setToolTipText("Export the current grid as a parsable input string");
		exportMapDialogMenuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent ev) {
				System.out.println(CoverageGUIDisplay.this.sim.getEnv().exportToString());
			}
		});
		fileMenu.add(exportMapDialogMenuItem);

		switchToHeadlessMenuItem.setToolTipText("Close this GUI and use headless mode (reopen the gui with \":setdisplay gui\")");
		switchToHeadlessMenuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent ev) {
				CoverageGUIDisplay.this.sim.getEngine().setDisplay(null);
			}
		});
		fileMenu.add(switchToHeadlessMenuItem);

		menuBar.add(fileMenu);
	}


	private void setupRunMenu(JMenuBar menuBar) {
		// Run menu
		final JMenu runMenu = new JMenu("Run");
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
				pauseCoverageMenuItem.setEnabled(true);
				runCoverageMenuItem.setEnabled(false);
				stepCoverageMenuItem.setEnabled(false);
				CoverageGUIDisplay.this.sim.getEngine().runSimulation();
				;
			}
		});
		runMenu.add(runCoverageMenuItem);

		pauseCoverageMenuItem.setToolTipText("Pause the simulation (if running)");
		pauseCoverageMenuItem.setAccelerator(KeyStroke.getKeyStroke("F7"));
		pauseCoverageMenuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent ev) {
				runCoverageMenuItem.setEnabled(true);
				stepCoverageMenuItem.setEnabled(true);
				pauseCoverageMenuItem.setEnabled(false);
				CoverageGUIDisplay.this.sim.getEngine().pauseSimulation();
			}
		});
		runMenu.add(pauseCoverageMenuItem);

		stepCoverageMenuItem.setToolTipText("Increment the coverage by one step");
		stepCoverageMenuItem.setAccelerator(KeyStroke.getKeyStroke("F8"));
		stepCoverageMenuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent ev) {
				CoverageGUIDisplay.this.sim.getEngine().stepSimulation();
			}
		});
		runMenu.add(stepCoverageMenuItem);

		restartCoverageMenuItem.setToolTipText("Reinitialize the current coverage");
		restartCoverageMenuItem.setAccelerator(KeyStroke.getKeyStroke("control R"));
		restartCoverageMenuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent ev) {
				runCoverageMenuItem.setEnabled(true);
				stepCoverageMenuItem.setEnabled(true);
				pauseCoverageMenuItem.setEnabled(false);
				CoverageGUIDisplay.this.sim.restartSimulation();
			}
		});
		runMenu.add(restartCoverageMenuItem);

		newCoverageMenuItem.setToolTipText("Start a new coverage");
		newCoverageMenuItem.setAccelerator(KeyStroke.getKeyStroke("control N"));
		newCoverageMenuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent ev) {
				runCoverageMenuItem.setEnabled(true);
				stepCoverageMenuItem.setEnabled(true);
				pauseCoverageMenuItem.setEnabled(false);
				CoverageGUIDisplay.this.sim.getEngine().newRun();
			}
		});
		runMenu.add(newCoverageMenuItem);

		menuBar.add(runMenu);
	}


	protected void openGridNodeEditDialog(final GridNode gridNode) {
		final JDialog dialog = new JDialog(this.frame, "Edit node (" + gridNode.getX() + ", " + gridNode.getY() + ")");
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
				CoverageGUIDisplay.this.mainPanel.repaint();
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
	 * Loads settings from the given file.
	 * 
	 * @param file
	 *                the file containing setting information
	 */
	public void showLoadSettingsDialog() {
		JFileChooser chooser = new JFileChooser();
		int status = chooser.showOpenDialog(CoverageGUIDisplay.this.frame);
		if (status == JFileChooser.APPROVE_OPTION) {
			File settingsFile = chooser.getSelectedFile();
			SimulatorMain.settings.loadFromFile(settingsFile);
		}
	}


	/**
	 * Exports settings to the given file, so they can be loaded later
	 * 
	 * @param settingsFile
	 *                the file to export to
	 */
	public void showExportSettingsDialog() {
		JFileChooser chooser = new JFileChooser();
		int status = chooser.showOpenDialog(CoverageGUIDisplay.this.frame);
		if (status == JFileChooser.APPROVE_OPTION) {
			File settingsFile = chooser.getSelectedFile();
			SimulatorMain.settings.exportToFile(settingsFile);
		}
	}


	@Override
	public void refresh() {
		this.mainPanel.repaint();
	}
}

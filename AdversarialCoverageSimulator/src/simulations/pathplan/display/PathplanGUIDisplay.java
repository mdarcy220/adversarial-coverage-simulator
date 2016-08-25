package simulations.pathplan.display;

import java.awt.GraphicsEnvironment;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.text.NumberFormat;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import adsim.Display;
import adsim.SimulatorMain;
import gridenv.GridNode;
import gridenv.NodeType;
import simulations.pathplan.PathplanSimulation;
import simulations.pathplan.display.PathplanGUIDisplay;
import simulations.pathplan.display.PathplanPanel;

public class PathplanGUIDisplay implements Display {
	private PathplanPanel mainPanel = null;
	private JFrame frame = null;
	private PathplanSimulation sim = null;


	private PathplanGUIDisplay(final PathplanSimulation sim) {
		this.sim = sim;
		this.frame = new JFrame("Adversarial Pathplan Simulator");
		// initialize the window
		this.frame.setSize(500, 400);
		this.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}


	public static PathplanGUIDisplay createInstance(final PathplanSimulation sim) {
		if (GraphicsEnvironment.isHeadless()) {
			System.err.println("Could not instantiate GUI display because environment is headless.");
			return null;
		}
		PathplanGUIDisplay instance = new PathplanGUIDisplay(sim);
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


	private PathplanPanel createMainPanel() {
		final PathplanPanel panel = new PathplanPanel(this.sim);
		panel.setSize(500, 500);
		panel.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				openGridNodeEditDialog(
						PathplanGUIDisplay.this.sim.getEnv().getGridNode(panel.getGridX(e.getX()), panel.getGridY(e.getY())));
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
				PathplanGUIDisplay.this.showExportSettingsDialog();
			}
		});
		fileMenu.add(exportSettingsMenuItem);

		loadSettingsMenuItem.setToolTipText("Load program settings from a file");
		loadSettingsMenuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent ev) {
				PathplanGUIDisplay.this.showLoadSettingsDialog();
			}
		});
		fileMenu.add(loadSettingsMenuItem);

		settingsDialogMenuItem.setToolTipText("Open the settings editor dialog");
		settingsDialogMenuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent ev) {
				SimulatorMain.settings.openSettingsDialog(PathplanGUIDisplay.this.frame);
				PathplanGUIDisplay.this.sim.getEnv().reloadSettings();
			}
		});
		fileMenu.add(settingsDialogMenuItem);

		exportMapDialogMenuItem.setToolTipText("Export the current grid as a parsable input string");
		exportMapDialogMenuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent ev) {
				System.out.println(PathplanGUIDisplay.this.sim.getEnv().exportToString());
			}
		});
		fileMenu.add(exportMapDialogMenuItem);

		switchToHeadlessMenuItem.setToolTipText("Close this GUI and use headless mode (reopen the gui with \":setdisplay gui\")");
		switchToHeadlessMenuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent ev) {
				PathplanGUIDisplay.this.sim.getEngine().setDisplay(null);
			}
		});
		fileMenu.add(switchToHeadlessMenuItem);

		menuBar.add(fileMenu);
	}


	private void setupRunMenu(JMenuBar menuBar) {
		// Run menu
		final JMenu runMenu = new JMenu("Run");
		final JMenuItem runPathplanMenuItem = new JMenuItem("Autorun");
		final JMenuItem pausePathplanMenuItem = new JMenuItem("Stop");
		final JMenuItem stepPathplanMenuItem = new JMenuItem("Step");
		final JMenuItem restartPathplanMenuItem = new JMenuItem("Restart (keep grid)");
		final JMenuItem newPathplanMenuItem = new JMenuItem("New Pathplan");

		runPathplanMenuItem.setToolTipText("Automatically step until Pathplan is done");
		runPathplanMenuItem.setAccelerator(KeyStroke.getKeyStroke("F6"));
		runPathplanMenuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent ev) {
				pausePathplanMenuItem.setEnabled(true);
				runPathplanMenuItem.setEnabled(false);
				stepPathplanMenuItem.setEnabled(false);
				PathplanGUIDisplay.this.sim.getEngine().runSimulation();
				;
			}
		});
		runMenu.add(runPathplanMenuItem);

		pausePathplanMenuItem.setToolTipText("Pause the simulation (if running)");
		pausePathplanMenuItem.setAccelerator(KeyStroke.getKeyStroke("F7"));
		pausePathplanMenuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent ev) {
				runPathplanMenuItem.setEnabled(true);
				stepPathplanMenuItem.setEnabled(true);
				pausePathplanMenuItem.setEnabled(false);
				PathplanGUIDisplay.this.sim.getEngine().pauseSimulation();
			}
		});
		runMenu.add(pausePathplanMenuItem);

		stepPathplanMenuItem.setToolTipText("Increment the Pathplan by one step");
		stepPathplanMenuItem.setAccelerator(KeyStroke.getKeyStroke("F8"));
		stepPathplanMenuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent ev) {
				PathplanGUIDisplay.this.sim.getEngine().stepSimulation();
			}
		});
		runMenu.add(stepPathplanMenuItem);

		restartPathplanMenuItem.setToolTipText("Reinitialize the current Pathplan");
		restartPathplanMenuItem.setAccelerator(KeyStroke.getKeyStroke("control R"));
		restartPathplanMenuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent ev) {
				runPathplanMenuItem.setEnabled(true);
				stepPathplanMenuItem.setEnabled(true);
				pausePathplanMenuItem.setEnabled(false);
				PathplanGUIDisplay.this.sim.restartSimulation();
			}
		});
		runMenu.add(restartPathplanMenuItem);

		newPathplanMenuItem.setToolTipText("Start a new Pathplan");
		newPathplanMenuItem.setAccelerator(KeyStroke.getKeyStroke("control N"));
		newPathplanMenuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent ev) {
				runPathplanMenuItem.setEnabled(true);
				stepPathplanMenuItem.setEnabled(true);
				pausePathplanMenuItem.setEnabled(false);
				PathplanGUIDisplay.this.sim.getEngine().newRun();
			}
		});
		runMenu.add(newPathplanMenuItem);

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

		final JLabel spreadLabel = new JLabel("Spread: ");
		settingsPanel.add(spreadLabel);
		final JFormattedTextField spreadField = new JFormattedTextField(doubleFormat);
		spreadField.setValue(new Double(gridNode.spreadability));
		settingsPanel.add(spreadField);

		final JLabel fuelLabel = new JLabel("Fuel: ");
		settingsPanel.add(fuelLabel);
		final JFormattedTextField fuelField = new JFormattedTextField(doubleFormat);
		fuelField.setValue(new Double(gridNode.dangerFuel));
		settingsPanel.add(fuelField);

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
				gridNode.spreadability = ((Number) spreadField.getValue()).doubleValue();
				gridNode.dangerFuel = ((Number) fuelField.getValue()).doubleValue();
				gridNode.setDangerProb(((Number) dangerField.getValue()).doubleValue());
				gridNode.setCost(((Number) costField.getValue()).doubleValue());
				gridNode.setCoverCount(((Number) coverCountSpinner.getValue()).intValue());
				gridNode.setNodeType(((ComboBoxNodeType) typeBox.getSelectedItem()).nodetype);
				PathplanGUIDisplay.this.mainPanel.repaint();
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
		int status = chooser.showOpenDialog(PathplanGUIDisplay.this.frame);
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
		int status = chooser.showOpenDialog(PathplanGUIDisplay.this.frame);
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

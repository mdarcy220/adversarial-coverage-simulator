package coveragegui;

import java.awt.*;
import javax.swing.*;

import adversarialcoverage.AdversarialCoverage;
import adversarialcoverage.CoverageEngine;
import adversarialcoverage.GridEnvironment;
import adversarialcoverage.GridNode;
import adversarialcoverage.GridRobot;
import adversarialcoverage.NodeType;

class CoveragePanel extends JPanel {
	private static final long serialVersionUID = -3413050706073789678L;

	GridEnvironment env;
	CoverageEngine engine;

	private boolean SHOW_BINARY_COVERAGE = AdversarialCoverage.settings.getBooleanProperty("display.show_binary_coverage");


	public CoveragePanel(CoverageEngine engine) {
		super();
		setSize(500, 500);
		this.engine = engine;
	}


	@Override
	public void paint(Graphics g) {
		g.clearRect(0, 0, this.getWidth(), this.getHeight());
		this.draw(g, this.getSize());
	}


	public void setEngine(CoverageEngine engine) {
		this.engine = engine;
	}


	private void draw(Graphics g, Dimension windowSize) {
		// Get the cell size
		Dimension cellSize = new Dimension(windowSize.width / this.engine.getEnv().gridSize.width,
				windowSize.height / this.engine.getEnv().gridSize.height);

		// Draw the grid
		g.translate(-cellSize.width, -cellSize.height);
		for (int x = 0; x < this.engine.getEnv().gridSize.width; x++) {
			g.translate(cellSize.width, 0);
			for (int y = 0; y < this.engine.getEnv().gridSize.height; y++) {
				g.translate(0, cellSize.height);
				drawGridCell(this.engine.getEnv().grid[x][y], g, cellSize);
			}
			g.translate(0, -this.engine.getEnv().gridSize.height * cellSize.height);
		}
		g.translate(-(this.engine.getEnv().gridSize.width - 1) * cellSize.width, cellSize.height);

		// Draw the robots
		g.setColor(Color.darkGray);
		for (GridRobot r : this.engine.getEnv().robots) {
			g.setColor(new Color((37 * r.getId()) % 256, (53 * (r.getId() + 9)) % 256, (71 * r.getId()) % 256));
			g.fillOval(r.getLocation().x * cellSize.width, r.getLocation().y * cellSize.height, cellSize.width, cellSize.height);
		}
	}


	private void drawGridCell(GridNode cell, Graphics g, Dimension cellSize) {
		int alpha = (int) (255 * 2 * cell.getDangerProb());
		if (255 < alpha) {
			alpha = 255;
		}
		g.setColor(new Color(255, 80, 255, alpha));
		g.fillRect(0, 0, cellSize.width, cellSize.height);
		g.setColor(Color.BLACK);
		if (cell.getNodeType() == NodeType.OBSTACLE) {
			g.fillRect(0, 0, cellSize.width, cellSize.height);
		}
		if (0 < cell.getCoverCount()) {
			g.setColor(new Color(0, 64, 0));
		} else {
			g.setColor(new Color(255, 20, 40));
		}

		if (this.SHOW_BINARY_COVERAGE) {
			g.drawString(cell.getCoverCount() < 1 ? "NO" : "YES", 0, cellSize.height);
		} else {
			g.drawString("" + cell.getCoverCount(), 0, cellSize.height);
		}
		g.setColor(Color.BLACK);

		g.drawRect(0, 0, cellSize.width, cellSize.height);
	}


	public int getGridX(int x) {
		return this.engine.getEnv().getWidth() * x / this.getWidth();
	}


	public int getGridY(int y) {
		return this.engine.getEnv().getHeight() * y / this.getHeight();
	}
}

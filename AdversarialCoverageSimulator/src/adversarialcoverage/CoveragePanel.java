package adversarialcoverage;
import java.awt.*;
import javax.swing.*;

class CoveragePanel extends JPanel {
	private static final long serialVersionUID = -3413050706073789678L;

	GridEnvironment env;
	CoverageEngine engine;


	public CoveragePanel(CoverageEngine engine) {
		super();
		setSize(500, 500);
		this.engine = engine;
	}


	@Override
	public void paint(Graphics g) {
		g.clearRect(0, 0, this.getWidth(), this.getHeight());
		this.engine.env.draw(g, this.getSize());
	}


	public void setEngine(CoverageEngine engine) {
		this.engine = engine;
	}


	public int getGridX(int x) {
		return this.env.getWidth() * x / this.getWidth();
	}


	public int getGridY(int y) {
		return this.env.getHeight() * y / this.getHeight();
	}
}

package adversarialcoverage;
import java.awt.*;
import javax.swing.*;

class CoveragePanel extends JPanel {
	private static final long serialVersionUID = -3413050706073789678L;

	GridEnvironment env;


	public CoveragePanel(GridEnvironment env) {
		super();
		setSize(500, 500);
		this.env = env;
	}


	@Override
	public void paint(Graphics g) {
		g.clearRect(0, 0, this.getWidth(), this.getHeight());
		this.env.draw(g, this.getSize());
	}


	public void setEnvironment(GridEnvironment env) {
		this.env = env;
	}


	public int getGridX(int x) {
		return this.env.getWidth() * x / this.getWidth();
	}


	public int getGridY(int y) {
		return this.env.getHeight() * y / this.getHeight();
	}
}

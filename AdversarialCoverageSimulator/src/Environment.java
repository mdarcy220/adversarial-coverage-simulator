import java.awt.Dimension;
import java.awt.Graphics;

public abstract class Environment {
	/**
	 * Draws a representation of this environment to the given {@code Graphics}
	 * object, within the bounds of the given window size.
	 * 
	 * @param g
	 *            the {@code Graphics} object to draw to
	 * @param windowSize
	 *            the size of the drawing area
	 */
	abstract void draw(Graphics g, Dimension windowSize);
}

package adversarialcoverage;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

public class Logger {
	private PrintWriter logWriter = new PrintWriter(System.out);


	public Logger() {
		super();
	}


	public PrintWriter getWriter() {
		return this.logWriter;
	}


	/**
	 * Logs the given message
	 * @param message the message to write into the log
	 */
	public void log(String message) {
		this.logWriter.println(message);
	}
	
	
	public void reloadSettings() {
		try {
			this.logWriter = new PrintWriter(new File(AdversarialCoverage.settings.getStringProperty("logging.logfile")));
		} catch (FileNotFoundException e) {
			System.err.println("Failed to open log file.");
		}
	}
}

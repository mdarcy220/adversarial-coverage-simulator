package adsim;

import java.util.*;

public class SimulatorArgs {
	private String[] origArgs = null;

	public boolean HEADLESS = false;
	public boolean USE_SETTINGS_FILE = false;
	public boolean USE_AUTOSTART = false;
	public boolean HAS_MAX_STEPS = true;

	public long MAX_STEPS = Long.MAX_VALUE;

	public String SETTINGS_FILE = null;
	public String RC_FILE = "";


	public SimulatorArgs() {
	}


	public SimulatorArgs(String[] origArgs) {
		this.origArgs = Arrays.copyOf(origArgs, origArgs.length);
		this.parseAll();
	}


	private void parseAll() {
		int argNum = 0;
		while (argNum < this.origArgs.length) {
			if (this.origArgs[argNum].equals("--headless")) {
				this.HEADLESS = true;
			} else if (this.origArgs[argNum].equals("--settings")) {
				argNum++;
				this.SETTINGS_FILE = this.origArgs[argNum];
				this.USE_SETTINGS_FILE = true;
			} else if (this.origArgs[argNum].equals("--autostart")) {
				this.USE_AUTOSTART = true;
			} else if (this.origArgs[argNum].equals("--maxsteps")) {
				argNum++;
				this.MAX_STEPS = Long.parseLong(this.origArgs[argNum]);
				this.HAS_MAX_STEPS = true;
			} else if (this.origArgs[argNum].equals("--rcfile")) {
				argNum++;
				this.RC_FILE = this.origArgs[argNum];
			}

			argNum++;
		}
	}
}

package adversarialcoverage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class TerminalDisplay implements DisplayAdapter {
	private CoverageEngine engine = null;
	private Thread inputThread = null;
	private Map<String, TerminalCommand> commandList = new HashMap<>();


	public TerminalDisplay() {
		this(null);
	}


	public TerminalDisplay(CoverageEngine engine) {
		this.engine = engine;
		this.inputThread = new Thread() {
			@Override
			public void run() {
				runInputLoop();
			}
		};
	}


	public void setEngine(CoverageEngine engine) {
		this.engine = engine;
	}


	public void setup() {
		this.registerDefaultCommands();
		this.inputThread.start();
		if (AdversarialCoverage.args.USE_AUTOSTART) {
			this.engine.runCoverage();
		}

	}


	private void registerDefaultCommands() {
		this.registerCommand(":help", new TerminalCommand() {
			@Override
			public void execute(String[] args) {
				System.out.println(getCommandList());
			}
		});

		this.registerCommand(":quit", new TerminalCommand() {
			@Override
			public void execute(String[] args) {
				// Do nothing. This is here so the :help command shows
				// :quit
			}
		});

		this.registerCommand(":pause", new TerminalCommand() {
			@Override
			public void execute(String[] args) {
				TerminalDisplay.this.engine.pauseCoverage();
			}
		});

		this.registerCommand(":step", new TerminalCommand() {
			@Override
			public void execute(String[] args) {
				TerminalDisplay.this.engine.stepCoverage();
			}
		});

		this.registerCommand(":run", new TerminalCommand() {
			@Override
			public void execute(String[] args) {
				TerminalDisplay.this.engine.runCoverage();
			}
		});

		this.registerCommand(":restart", new TerminalCommand() {
			@Override
			public void execute(String[] args) {
				TerminalDisplay.this.engine.restartCoverage();
			}
		});

		this.registerCommand(":new", new TerminalCommand() {
			@Override
			public void execute(String[] args) {
				TerminalDisplay.this.engine.newCoverage();
			}
		});

		this.registerCommand(":showstate", new TerminalCommand() {
			@Override
			public void execute(String[] args) {
				System.out.printf("isRunning = %s\n", TerminalDisplay.this.engine.isRunning());
			}
		});

		this.registerCommand(":set", new TerminalCommand() {
			@Override
			public void execute(String[] args) {
				if (args.length < 2) {
					return;
				}
				AdversarialCoverage.settings.setAuto(args[0], args[1]);
				TerminalDisplay.this.engine.getEnv().reloadSettings();
			}
		});

		this.registerCommand(":showsettings", new TerminalCommand() {
			@Override
			public void execute(String[] args) {
				if (0 < args.length && args[0].equals("ascommands")) {
					System.out.println(AdversarialCoverage.settings.exportToCommandString());
				} else {
					System.out.println(AdversarialCoverage.settings.exportToString());
				}
			}
		});
	}


	private void runInputLoop() {
		Scanner inReader = new Scanner(System.in);
		while (true) {
			List<String> argList = splitCmdStr(inReader.nextLine());
			if (argList.size() == 0) {
				continue;
			}

			String command = argList.get(0);
			if (command.equals(":quit")) {
				this.engine.pauseCoverage();
				break;
			}

			String[] args = new String[argList.size() - 1];
			for (int i = 1; i < argList.size(); i++) {
				args[i - 1] = argList.get(i);
			}
			executeCommand(command, args);

		}
		inReader.close();
	}


	private void executeCommand(String commandName, String[] args) {
		TerminalCommand cmd = this.commandList.get(commandName);
		if (cmd != null) {
			cmd.execute(args);
		} else {
			System.err.println("Invalid command name. Run :help to see a list of commands.");
		}
	}


	private void registerCommand(String command, TerminalCommand action) {
		this.commandList.put(command, action);
	}


	public String getCommandList() {
		StringBuilder cmdList = new StringBuilder();
		for (String key : this.commandList.keySet()) {
			cmdList.append(key);
			cmdList.append('\n');
		}
		return cmdList.toString();
	}


	private List<String> splitCmdStr(String cmdStr) {
		if (cmdStr == null || cmdStr.length() == 0) {
			return new ArrayList<>();
		}

		List<String> argList = new ArrayList<>();
		StringBuilder curArg = new StringBuilder("");

		int pos = 0;
		boolean inQuote = false;

		while (pos < cmdStr.length()) {
			char curChar = cmdStr.charAt(pos);

			if (curChar == '\\') {

				if ((pos + 1) < cmdStr.length()) {
					curArg.append(escapeChar(cmdStr.charAt(pos + 1)));
				} else {
					// Reached end of string
					curArg.append(curChar);
				}
				pos++;

			} else if (isQuoteChar(curChar)) {

				inQuote = !inQuote;

			} else if (inQuote || (!Character.isWhitespace(curChar))) {

				curArg.append(curChar);

			} else if (Character.isWhitespace(curChar) && (0 < curArg.length())) {

				argList.add(curArg.toString());
				curArg.setLength(0);

			}

			pos++;
		}
		
		if(0 < curArg.length()) {
			argList.add(curArg.toString());
			curArg.setLength(0);
		}

		return argList;
	}


	private char escapeChar(char c) {
		switch (c) {
		case 'n':
			return '\n';
		case 'r':
			return '\r';
		case 't':
			return '\t';
		case 'b':
			return '\b';
		default:
			return c;
		}
	}


	private boolean isQuoteChar(char c) {
		return (c == '\'' || c == '"');
	}


	@Override
	public void refresh() {
		// TODO: Print text-based grid representation
	}

}


interface TerminalCommand {
	public void execute(String[] args);
}

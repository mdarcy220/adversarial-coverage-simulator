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
				// Do nothing. This is here so the :help command shows :quit
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
	}


	private void runInputLoop() {
		Scanner inReader = new Scanner(System.in);
		while (true) {
			String line = "";
			line = inReader.nextLine().trim();
			Scanner lineScanner = new Scanner(line);
			if (!lineScanner.hasNext()) {
				continue;
			}
			String command = lineScanner.next();
			List<String> argsList = new ArrayList<>();
			while (lineScanner.hasNext()) {
				argsList.add(lineScanner.next());
			}
			lineScanner.close();

			if (command.equals(":quit")) {
				this.engine.pauseCoverage();
				break;
			}
			String[] args = new String[argsList.size()];
			argsList.toArray(args);
			handleCommand(command, args);

		}
		inReader.close();
	}


	private void handleCommand(String commandName, String[] args) {
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


	@Override
	public void refresh() {
		// TODO: Print text-based grid representation
	}

}


interface TerminalCommand {
	public void execute(String[] args);
}

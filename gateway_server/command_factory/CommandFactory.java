package gateway_server.command_factory;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class CommandFactory {
	private final Map<String, Function<JSONObject, Boolean>> commandMap = new HashMap<>();
	private static final CommandFactory commandFactory = new CommandFactory();

	private CommandFactory() {}

	public static CommandFactory getInstance() {
		return commandFactory;
	}

	public void addCommand(String className, Function<JSONObject, Boolean> operation) {
		commandMap.put(className, operation);
	}
	public void removeCommand(String className) {
		commandMap.remove(className);
	}

	public Function<JSONObject, Boolean> getCommand(String className) {
		return commandMap.get(className);

	}
}


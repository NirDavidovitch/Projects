package gateway_server.dir_monitor;

import gateway_server.command_factory.CommandFactory;
import org.json.JSONObject;

import java.util.function.Function;

public class IOTCommandObserver implements DirectoryObserver {
	private final CommandFactory commandFactory;

	public IOTCommandObserver(CommandFactory commandFactory) {
		this.commandFactory = commandFactory;
	}

	@Override
	public void onFileAdded(Class<?> loadedClass) {
		if (Function.class.isAssignableFrom(loadedClass)) {
			try {
				Function<JSONObject, Boolean> operation = (Function<JSONObject, Boolean>) loadedClass.newInstance();
				// Add the operation to the CommandFactory
				commandFactory.addCommand(loadedClass.getSimpleName(), operation);
				System.out.println("Added new IOTOperation: " + loadedClass.getSimpleName());
			} catch (InstantiationException | IllegalAccessException e) {
				System.err.println("Failed to instantiate IOTOperation: " + loadedClass.getSimpleName());
				e.printStackTrace();
			}
		} else {
			System.err.println("Loaded class does not implement IOTOperation: " + loadedClass.getSimpleName());
		}
	}

	@Override
	public void onFileRemoved(Class<?> loadedClass) {
		if (Function.class.isAssignableFrom(loadedClass)) {
			// Remove the operation from the CommandFactory
			commandFactory.removeCommand(loadedClass.getSimpleName());
			System.out.println("Removed IOTOperation: " + loadedClass.getSimpleName());
		}
	}
}


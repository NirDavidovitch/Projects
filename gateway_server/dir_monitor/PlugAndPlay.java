package gateway_server.dir_monitor;

import gateway_server.command_factory.CommandFactory;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.*;
import java.util.List;
import java.util.function.Function;

public class PlugAndPlay implements Runnable{
	private WatchService watchService = FileSystems.getDefault().newWatchService();
	private boolean isRunning = true;
	private final String directoryPath;
	private final DynamicJarLoader dynamicJarLoader;
	private final CommandFactory commandFactory;

	public PlugAndPlay(CommandFactory factory, String interfaceName, String directoryPath) throws IOException {
		this.commandFactory = factory;
		this.directoryPath = directoryPath;
		dynamicJarLoader = new DynamicJarLoader(interfaceName);
	}

	@Override
	public void run() {
		Path directory = Paths.get(directoryPath);
		List<Class<?>> loadedCommands;
		try {
			directory.register(watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		while(isRunning && !Thread.currentThread().isInterrupted()) {
			WatchKey key;
			try {
				key = watchService.take();
			} catch (InterruptedException e) {
				e.printStackTrace();
				isRunning = false;
				continue;
			}

			for (WatchEvent<?> event : key.pollEvents()) {
				WatchEvent.Kind<?> kind = event.kind();
				if (kind.equals(StandardWatchEventKinds.ENTRY_CREATE)) {
					WatchEvent<Path> ep = cast(event);
					Path jarName = ep.context();

					try {
						loadedCommands = dynamicJarLoader.load(directoryPath + "/" + jarName.toString());
					} catch (IOException | ClassNotFoundException e) {
						throw new RuntimeException(e);
					}

					for (Class<?> command : loadedCommands) {
						Function<JSONObject, Boolean> newCommand = null;
						try {
							newCommand = (Function<JSONObject, Boolean>) command.getDeclaredConstructor().newInstance();
						} catch (InstantiationException | IllegalAccessException | InvocationTargetException |
								 NoSuchMethodException e) {
							throw new RuntimeException(e);
						}

						String commandName = jarName.getFileName().toString();
						commandName = commandName.replace(".jar", "");

						commandFactory.addCommand(commandName, newCommand);
					}
				} else if (kind.equals(StandardWatchEventKinds.ENTRY_DELETE)) {

				} else {

				}

			}
		}
	}

	private static <T> WatchEvent<T> cast(WatchEvent<?> event) {
		return (WatchEvent<T>) event;
	}
}

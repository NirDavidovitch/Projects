package gateway_server.dir_monitor;

import org.json.JSONObject;

import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class DirectoryMonitor {
	private final List<DirectoryObserver> observers = new ArrayList<>();

	public void addObserver(DirectoryObserver observer) {
		observers.add(observer);
	}

	public void startMonitoring(String directoryPath) throws Exception {
		Path directory = Paths.get(directoryPath);
		WatchService watchService = FileSystems.getDefault().newWatchService();
		directory.register(watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE);

		while (true) {
			WatchKey key;
			try {
				key = watchService.take();
			} catch (InterruptedException e) {
				return;
			}

			for (WatchEvent<?> event : key.pollEvents()) {
				WatchEvent.Kind<?> kind = event.kind();
				if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
					WatchEvent<Path> ev = (WatchEvent<Path>) event;
					Path filePath = directory.resolve(ev.context());

					// Load the class dynamically using DynamicJarLoader
					List<Class<?>> loadedClasses = new DynamicJarLoader(filePath.toString(), "Function").load();

					for (Class<?> loadedClass : loadedClasses) {
						if (Function.class.isAssignableFrom(loadedClass)) {
							for (DirectoryObserver observer : observers) {
								observer.onFileAdded(loadedClass);
							}
						} else {
							System.err.println("Loaded class does not implement Function: " + loadedClass.getSimpleName());
						}
					}

					// Handle other interfaces and observers similarly if needed
					// ...

				} else if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
					WatchEvent<Path> ev = (WatchEvent<Path>) event;
					Path filePath = directory.resolve(ev.context());

					// Load the class dynamically using DynamicJarLoader
					List<Class<?>> loadedClasses = new DynamicJarLoader(filePath.toString(), "Function").load();

					// Notify observers for each loaded class implementing IOTOperation
					for (Class<?> loadedClass : loadedClasses) {
						if (Function.class.isAssignableFrom(loadedClass)) {
							for (DirectoryObserver observer : observers) {
								observer.onFileRemoved(loadedClass);
							}
						}
					}

					// Handle other interfaces and observers similarly if needed
					// ...
				}
			}

			boolean valid = key.reset();
			if (!valid) {
				break;
			}
		}
	}
}

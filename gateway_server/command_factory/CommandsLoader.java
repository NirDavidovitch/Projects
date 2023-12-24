package gateway_server.command_factory;

import gateway_server.commands.RegCompany;
import gateway_server.commands.IotUpdate;
import gateway_server.commands.RegIot;
import org.json.JSONObject;

import java.io.File;
import java.util.function.Function;

public class CommandsLoader {
	private static final CommandFactory commandFactory = CommandFactory.getInstance();

	public static void loadFunctionsFromDirectory() {
		// commandFactory.addCommand("Print", new Print());
		commandFactory.addCommand("Update", new IotUpdate());
		commandFactory.addCommand("RegCompany", new RegCompany());
		commandFactory.addCommand("RegIot", new RegIot());
	}

	public static void loadFunctionsFromDirectory(String directoryPath) {
		File directory = new File(directoryPath);
		if (!directory.isDirectory()) {
			System.err.println("Invalid directory path.");
			return;
		}

		File[] files = directory.listFiles();
		if (files == null) {
			System.err.println("Error listing files in the directory.");
			return;
		}

		for (File file : files) {
			if (file.isFile() && file.getName().endsWith(".class")) {
				try {
					String className = getClassName(file);
					Class<?> loadedClass = Class.forName(className);

					if (Function.class.isAssignableFrom(loadedClass)) {
						Class<Function<JSONObject, Boolean>> functionClass =
							(Class<Function<JSONObject, Boolean>>) loadedClass;

						// Create an instance of the class and add it to the factory
						Function<JSONObject, Boolean> functionInstance = functionClass.getDeclaredConstructor().newInstance();
						commandFactory.addCommand(className, functionInstance);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	private static String getClassName(File file) {
		String fileName = file.getName();
		return fileName.substring(0, fileName.lastIndexOf('.'));
	}
}

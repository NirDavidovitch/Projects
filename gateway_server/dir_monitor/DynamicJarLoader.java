package gateway_server.dir_monitor;

import java.io.*;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

public class DynamicJarLoader {
	private String jarPath = null;
	private String interfaceName = null;

	public DynamicJarLoader(String jarPath, String interfaceName) throws IOException, ClassNotFoundException {
		this.interfaceName = interfaceName;
		this.jarPath = jarPath;
	}

	public List<Class<?>> load() throws IOException, ClassNotFoundException {
		JarInputStream jarInputStream = new JarInputStream(new FileInputStream(jarPath));
		List<Class<?>> classList = new ArrayList<>();
		JarEntry entry = null;
		File jarFile = new File(jarPath);
		URL[] urls = new URL[1];

		urls[0] = jarFile.toURI().toURL();
		URLClassLoader loader = new URLClassLoader(urls);
		Class<?> myInterface = loader.loadClass(interfaceName);

		while ((entry = jarInputStream.getNextJarEntry()) != null) {
			String className = entry.getName().replace(".class", "");
			Class<?> currClass = loader.loadClass(className);
			if (myInterface.isAssignableFrom(currClass) && !className.equals(interfaceName)) {
				classList.add(currClass);
			}
		}

		loader.close();
		return classList;
	}
}
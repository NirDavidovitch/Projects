package gateway_server.dir_monitor;

public interface DirectoryObserver {
	void onFileAdded(Class<?> loadedClass);

	void onFileRemoved(Class<?> loadedClass);
}
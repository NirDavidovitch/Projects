package gateway_server;

import gateway_server.command_factory.CommandFactory;
import gateway_server.command_factory.CommandsLoader;
import gateway_server.connection_manager.ConnectionManager;
import gateway_server.dir_monitor.DirectoryMonitor;
import gateway_server.dir_monitor.IOTCommandObserver;
import gateway_server.thread_pool.*;

import org.json.JSONObject;

import java.io.IOException;
import java.util.function.Consumer;
import java.util.function.Function;

public class GatewayServer {
	private final ConnectionManager<JSONObject> connectionManager;
	private final ThreadPool threadPool;
	private final Priority[] priorities = {Priority.LOW, Priority.DEFAULT, Priority.HIGH};

	public GatewayServer(int[] udpPorts, int[] tcpPorts) throws IOException {
		connectionManager = new ConnectionManager<>(udpPorts, tcpPorts, new AddRequestToPool());
		threadPool = new ThreadPool(Runtime.getRuntime().availableProcessors());
		CommandsLoader.loadFunctionsFromDirectory(); //load/init
	}

	public void run() {
		connectionManager.run();
	}

	public void shutDown() {
		connectionManager.shutDown();
	}

	class AddRequestToPool implements Consumer<JSONObject> {

		@Override
		public void accept(JSONObject json) {
			threadPool.submit(new RequestHandler(json), priorities[json.getInt("priority")]);
		}
	}

	static class RequestHandler implements Runnable {
		JSONObject json = null;

		public RequestHandler(JSONObject json) {
			this.json = json;
		}

		@Override
		public void run() {
			String className = json.getString("command");
			Function<JSONObject, Boolean> command = CommandFactory.getInstance().getCommand(className);

			if (command != null) {
				command.apply(json);
			} else {
				System.out.println("Command " + className + " Not Found");
			}
		}
	}

	private void registerDirectoryMonitor() {
		DirectoryMonitor directoryMonitor = new DirectoryMonitor();

		directoryMonitor.addObserver(new IOTCommandObserver(CommandFactory.getInstance()));
		Thread dirMonitorThread = new DirMonitorThread(directoryMonitor);

		dirMonitorThread.start();
	}

	private static class DirMonitorThread extends Thread {
		private final DirectoryMonitor directoryMonitor;

		DirMonitorThread(DirectoryMonitor directoryMonitor) {
			this.directoryMonitor = directoryMonitor;
		}

		@Override
		public void run() {
			try {
				directoryMonitor.startMonitoring("/home/nird221/git/fs/final_project/iot_infrastructure/src/main/java/gateway_server/commands");
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}
}

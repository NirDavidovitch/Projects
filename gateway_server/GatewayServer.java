package gateway_server;

import gateway_server.command_factory.CommandFactory;
import gateway_server.command_factory.CommandsLoader;
import gateway_server.connection_manager.ConnectionManager;
import gateway_server.dir_monitor.IOTCommandObserver;
import gateway_server.dir_monitor.PlugAndPlay;
import gateway_server.mongodb.MongoHandler;
import gateway_server.thread_pool.*;

import org.json.JSONObject;

import java.io.IOException;
import java.util.function.Consumer;
import java.util.function.Function;

public class GatewayServer {
	private final ConnectionManager<JSONObject> connectionManager;
	private final ThreadPool threadPool;
	private final Priority[] priorities = {Priority.LOW, Priority.DEFAULT, Priority.HIGH};
	private final Thread dirMonitorThread;

	public GatewayServer(int[] udpPorts, int[] tcpPorts, String mongoConnectionString) throws IOException {
		connectionManager = new ConnectionManager<>(udpPorts, tcpPorts, new AddRequestToPool());
		threadPool = new ThreadPool(Runtime.getRuntime().availableProcessors());

		MongoHandler.getMongoHandler().setConnectionString(mongoConnectionString);
		PlugAndPlay plugAndPlay = new PlugAndPlay(CommandFactory.getInstance(), "java.util.function.Function", "/home/nird221/git/fs/final_project/iot_infrastructure/src/main/java/gateway_server/commands");
		CommandsLoader.loadFunctionsFromDirectory(); //load/init

		dirMonitorThread = new Thread(plugAndPlay);
	}

	public void run() {
		dirMonitorThread.start();
		connectionManager.run();
	}

	public void shutDown() {
		dirMonitorThread.interrupt();
		connectionManager.shutDown();
	}


	/* Activated threw the ConnectionManager (when a tcp/udp request received) to get the JSON and register it to the ThreadPool */
	class AddRequestToPool implements Consumer<JSONObject> {

		@Override
		public void accept(JSONObject json) {
			threadPool.submit(new RequestHandler(json), priorities[json.getInt("priority")]);
		}
	}

	/* The Handler used by the thread to handle and parse the JSON, getting the command from the Factory, and run it using apply(json) which starts the relevant command */
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
}

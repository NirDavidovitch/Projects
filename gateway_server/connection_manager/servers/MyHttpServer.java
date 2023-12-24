package gateway_server.connection_manager.servers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class MyHttpServer implements Runnable {
	private HttpServer httpServer = null;
	private final int httpPort;
	private final Consumer<IPeer> handler;

	public MyHttpServer(int httpPort, Consumer<IPeer> handler) {
		this.httpPort = httpPort;
		this.handler = handler;
	}

	@Override
	public void run() {
		try {
			startHttpServer();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void startHttpServer() throws IOException {
		httpServer = HttpServer.create(new InetSocketAddress(httpPort), 0);
		httpServer.createContext("/gate", this::handleHttpRequest);
		httpServer.setExecutor(Executors.newSingleThreadExecutor());
		httpServer.start();
	}

	private void handleHttpRequest(HttpExchange exchange) throws IOException {
		handler.accept(new HTTPPeer(exchange));
	}

	public void stop() {
		httpServer.stop(100);
	}
}
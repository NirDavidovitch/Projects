package gateway_server.connection_manager;

import gateway_server.connection_manager.servers.peer_exceptions.PeerCloseException;
import gateway_server.connection_manager.servers.peer_exceptions.PeerReadException;
import org.json.JSONException;
import org.json.JSONObject;
import gateway_server.connection_manager.servers.*;

import java.io.IOException;
import java.util.function.Consumer;

public class ConnectionManager<T> {
	private final TcpUdpServer tcpUdpServer;
	private final MyHttpServer httpServer;
	private final Consumer<T> businessHandler;
	private final ITranslator<T> translator;
	private final int[] tcpPorts;
	private final int[] udpPorts;

	public ConnectionManager(int[] udpPorts, int[] tcpPorts, Consumer<JSONObject> businessHandler) throws IOException {
		this(udpPorts, tcpPorts, (Consumer<T>) businessHandler, (ITranslator<T>) new JSONTranslator());
	}

	/* constructor to the ConnectionManager in case the user wants to use custom translator (instead of JSON) */
	public ConnectionManager(int[] udpPorts, int[] tcpPorts, Consumer<T> businessHandler, ITranslator<T> translator) throws IOException {
		this.tcpPorts = tcpPorts;
		this.udpPorts = udpPorts;
		this.businessHandler = businessHandler;
		this.translator = translator;

		Consumer<IPeer> requestHandler = new RequestHandler();
		this.tcpUdpServer = new TcpUdpServer(requestHandler);
		this.httpServer = new MyHttpServer(8080, requestHandler);
	}

	/* runs the tcp/udp server (multiProtocolServer) on the given ports */
	public void run() {
		try {
			for (int port : tcpPorts) {
				tcpUdpServer.addTcpConnection(port);
			}
			for (int port : udpPorts) {
				tcpUdpServer.addUdpConnection(port);
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		httpServer.run();
		tcpUdpServer.run();
	}

	class RequestHandler implements Consumer<IPeer> {

		@Override
		public void accept(IPeer peer) {
			T translatedRequest = null;

			try {
				translatedRequest = translator.translate(peer.read(2048));
			} catch (JSONException | PeerReadException e) {
				try {
					peer.close();
				} catch (PeerCloseException a) {
					throw new RuntimeException(a);
				}
			}

			if (translatedRequest == null) {
				try {
					peer.close();
				} catch (PeerCloseException e) {
					throw new RuntimeException(e);
				}
				return;
			}

			businessHandler.accept(translatedRequest);
		}
	}

	public void shutDown() {
		this.tcpUdpServer.stop();
	}
}

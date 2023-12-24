package gateway_server.connection_manager.servers;

import com.sun.net.httpserver.HttpExchange;
import gateway_server.connection_manager.servers.peer_exceptions.PeerReadException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class HTTPPeer implements IPeer {
	private final HttpExchange httpExchange;

	public HTTPPeer(HttpExchange httpExchange) {
		this.httpExchange = httpExchange;
	}

	@Override
	public byte[] read(int bytesToRead) throws PeerReadException {
		InputStream requestBody = httpExchange.getRequestBody();

		ByteBuffer buffer = ByteBuffer.allocate(1024);
		int bytesRead;

		try {
			while ((bytesRead = requestBody.read(buffer.array())) != -1) {
				buffer.position(bytesRead);
			}
		} catch (IOException e) {
			throw new PeerReadException(e);
		}
		return buffer.array();
	}

	@Override
	public void write(byte[] arr) {
		// httpExchange.sendResponseHeaders();
	}

	@Override
	public void close() {
	}
}
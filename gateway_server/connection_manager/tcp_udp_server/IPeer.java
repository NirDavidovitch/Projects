package gateway_server.connection_manager.tcp_udp_server;

import gateway_server.connection_manager.tcp_udp_server.peer_exceptions.*;

public interface IPeer {
	byte[] read(int bytesToRead) throws PeerReadException;
	void write(byte[] arr) throws PeerWriteException;
	void close() throws PeerCloseException;
}


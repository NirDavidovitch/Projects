package gateway_server.connection_manager.servers;

import gateway_server.connection_manager.servers.peer_exceptions.*;

public interface IPeer {
	byte[] read(int bytesToRead) throws PeerReadException;
	void write(byte[] arr) throws PeerWriteException;
	void close() throws PeerCloseException;
}


package gateway_server.connection_manager.tcp_udp_server;

import gateway_server.connection_manager.tcp_udp_server.peer_exceptions.*;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

public class UDPPeer implements IPeer {
	private final DatagramChannel datagramChannel;
	private SocketAddress clientAddress = null;

	UDPPeer(DatagramChannel datagramChannel) {
		this.datagramChannel = datagramChannel;
	}

	@Override
	public byte[] read(int bytesToRead) throws PeerReadException {
		ByteBuffer byteBuffer = ByteBuffer.allocate(bytesToRead);
		try {
			this.clientAddress = datagramChannel.receive(byteBuffer);
		} catch (IOException e) {
			throw new PeerReadException(e);
		}

		byteBuffer.flip();

		byte[] data = new byte[byteBuffer.limit()];
		byteBuffer.get(data);

		return data;
	}
	@Override
	public void write(byte[] arr) throws PeerWriteException {
		ByteBuffer byteBuffer = ByteBuffer.wrap(arr);

		try {
			datagramChannel.send(byteBuffer, clientAddress);
		} catch (IOException e) {
			throw new PeerWriteException(e);
		}
	}
	@Override
	public void close() throws PeerCloseException {
		try {
			datagramChannel.close();
		} catch (IOException e) {
			throw new PeerCloseException(e);
		}
	}
}

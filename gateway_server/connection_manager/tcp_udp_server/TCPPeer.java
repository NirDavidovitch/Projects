package gateway_server.connection_manager.tcp_udp_server;

import gateway_server.connection_manager.tcp_udp_server.peer_exceptions.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

public class TCPPeer implements IPeer {
	private final SocketChannel socketChannel;
	private final SelectionKey key;

	TCPPeer(SocketChannel socketChannel, SelectionKey key) {
		this.socketChannel = socketChannel;
		this.key = key;
	}

	@Override
	public byte[] read(int bytesToRead) throws PeerReadException {
		ByteBuffer byteBuffer = ByteBuffer.allocate(bytesToRead);
		int numOfReads = 0;

		try {
			numOfReads = socketChannel.read(byteBuffer);

		} catch (IOException e) {
			throw new PeerReadException(e);
		}

		if (numOfReads == -1) {
			try {
				socketChannel.close();
			} catch (IOException e) {
				throw new PeerReadException(e);
			}
			key.cancel();
			return "TCP client Disconnected".getBytes();
		}

		byteBuffer.flip();

		byte[] data = new byte[byteBuffer.limit()];
		byteBuffer.get(data);

		return data;
	}
	@Override
	public void write(byte[] arr) throws PeerWriteException {
		if (!key.isValid()) {
			return;
		}

		ByteBuffer byteBuffer = ByteBuffer.wrap(arr);

		try {
			socketChannel.write(byteBuffer);
		} catch (IOException e) {
			throw new PeerWriteException(e);
		}
	}

	@Override
	public void close() throws PeerCloseException {
		try {
			socketChannel.close();
		} catch (IOException e) {
			throw new PeerCloseException(e);
		}
	}
}

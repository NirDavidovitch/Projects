package gateway_server.connection_manager.servers;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.Set;
import java.util.function.Consumer;

public class TcpUdpServer implements Runnable {
	private boolean isStopped = false;
	private final Selector selector = Selector.open();
	private final Consumer<IPeer> handler;

	public TcpUdpServer(Consumer<IPeer> handler) throws IOException {
		this.handler = handler;
	}

	@Override
	public void run() {
		try {
			while (!isStopped) {
				selector.select(10000);
				Set<SelectionKey> keys = selector.selectedKeys();

				for (Iterator<SelectionKey> i = keys.iterator(); i.hasNext(); ) {
					SelectionKey key = i.next();
					i.remove();

					if (key.isAcceptable()) {
						establishTCPConnection(key);
					} else if (key.isReadable()) {
						handler.accept((IPeer) key.attachment());
					}
				}
			}

			Set<SelectionKey> keys = selector.keys();

			for (Iterator<SelectionKey> i = keys.iterator(); i.hasNext(); ) {
				SelectionKey key = i.next();
				i.remove();

				try { key.channel().close(); } catch (IOException ignore) {}
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void addTcpConnection(int port) throws IOException {
		ServerSocketChannel tcpChannel = ServerSocketChannel.open();

		tcpChannel.bind(new InetSocketAddress(port));
		tcpChannel.configureBlocking(false);
		tcpChannel.register(selector, SelectionKey.OP_ACCEPT);
	}

	public void addUdpConnection(int port) throws IOException {
		DatagramChannel udpChannel = DatagramChannel.open();

		udpChannel.bind(new InetSocketAddress(port));
		udpChannel.configureBlocking(false);
		udpChannel.register(selector, SelectionKey.OP_READ, new UDPPeer(udpChannel));
	}

	public void stop() {
		isStopped = true;
	}
	private void establishTCPConnection(SelectionKey key) throws IOException {
		ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
		SocketChannel socketChannel = serverSocketChannel.accept();

		socketChannel.configureBlocking(false);
		socketChannel.register(selector, SelectionKey.OP_READ, new TCPPeer(socketChannel, key));
	}
}
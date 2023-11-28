package gateway_server.connection_manager;

public interface ITranslator<T> {
	T translate(byte[] request);
}

package gateway_server.commands;

import gateway_server.mongodb.MongoHandler;
import org.json.JSONObject;

import java.util.function.Function;

import static gateway_server.commands.CONST.IoTUpdatesCollection;

public class IotUpdate implements Function<JSONObject, Boolean> {
	@Override
	public Boolean apply(JSONObject jsonObject) {
		MongoHandler mongoHandler = MongoHandler.getMongoHandler();

		mongoHandler.createDocument(jsonObject.getString("company"), IoTUpdatesCollection, jsonObject);

		return true;
	}
}

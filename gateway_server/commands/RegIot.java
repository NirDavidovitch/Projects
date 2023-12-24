package gateway_server.commands;

import gateway_server.mongodb.MongoHandler;
import org.json.JSONObject;

import java.util.function.Function;

public class RegIot implements Function<JSONObject, Boolean> {
	@Override
	public Boolean apply(JSONObject jsonObject) {

		MongoHandler.getMongoHandler().createDocument(jsonObject.getString("company"), "iots", jsonObject);

		return true;
	}
}

package gateway_server.commands;

import gateway_server.mongodb.MongoHandler;
import org.json.JSONObject;

import java.util.function.Function;

import static gateway_server.commands.CONST.IoTCollection;
import static gateway_server.commands.CONST.IoTUpdatesCollection;

public class RegCompany implements Function<JSONObject, Boolean> {
	@Override
	public Boolean apply(JSONObject jsonObject) {
		// JSONObject validationSchema = jsonObject.getJSONObject("validation");
		String company = jsonObject.getString("name");

		MongoHandler.getMongoHandler().createCollection(company, IoTCollection);
		MongoHandler.getMongoHandler().createCollection(company, IoTUpdatesCollection);
		return true;
	}
}

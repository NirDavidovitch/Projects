package gateway_server.commands;

import org.json.JSONObject;

import java.util.function.Function;

public class Print implements Function<JSONObject, Boolean> {
	@Override
	public Boolean apply(JSONObject jsonObject) {
		String commandDetails = jsonObject.getString("details");

		System.out.println("the command details are: " + commandDetails);

		return true;
	}
}

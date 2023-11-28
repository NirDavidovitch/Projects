package gateway_server.connection_manager;

import org.json.JSONException;
import org.json.JSONObject;

public class JSONTranslator implements ITranslator<JSONObject> {

	public JSONObject translate(byte[] bytes) throws JSONException{
		String str = null;
		JSONObject resJson = null;

		try {
			str = new String(bytes);
			resJson = new JSONObject(str);
		} catch (JSONException e) {
			throw new JSONException(e);
		}

		return resJson;
	}

}

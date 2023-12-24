package gateway_server.mongodb;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.CreateCollectionOptions;
import com.mongodb.client.model.ValidationAction;
import com.mongodb.client.model.ValidationLevel;
import com.mongodb.client.model.ValidationOptions;
import org.bson.Document;
import org.json.JSONObject;

public class MongoHandler {
	private static final MongoHandler mongoHandler = new MongoHandler();
	private MongoClient mongoClient = null;

	private MongoHandler() {}

	public static MongoHandler getMongoHandler() {
		return mongoHandler;
	}
	public void setConnectionString(String connectionString) {
		this.mongoClient = MongoClients.create(connectionString);
	}

	public void createDB(JSONObject json) {
		// mongoClient.d
	}

	public void createCollection(String dbName, String collectionName, JSONObject validationRules) {
		MongoDatabase database = mongoClient.getDatabase(dbName);
		Document validationDocument = Document.parse(validationRules.toString());

		ValidationOptions validationOptions = new ValidationOptions()
			.validator(validationDocument)
			.validationLevel(ValidationLevel.MODERATE)
			.validationAction(ValidationAction.ERROR);

		CreateCollectionOptions collectionOptions = new CreateCollectionOptions()
			.validationOptions(validationOptions);

		database.createCollection(collectionName, collectionOptions);
	}

	public void createCollection(String dbName, String collectionName) {
		MongoDatabase database = mongoClient.getDatabase(dbName);

		database.createCollection(collectionName);
	}

	public void createDocument(String dbName, String collectionName, JSONObject document) {
		MongoDatabase database = mongoClient.getDatabase(dbName);

		database.getCollection(collectionName).insertOne(new Document(document.toMap()));
	}

	public void updateDocument(JSONObject json) {
		//TODO
	}

	public void readFromCollection(JSONObject json) {
		//TODO
	}

	public void deleteDocument(JSONObject json) {
		//TODO
	}

	public void deleteCollection(JSONObject json) {
		//TODO
	}

	public void deleteDB(JSONObject json) {
		//TODO
	}

	public void closeConnection() {
		this.mongoClient.close();
	}
}

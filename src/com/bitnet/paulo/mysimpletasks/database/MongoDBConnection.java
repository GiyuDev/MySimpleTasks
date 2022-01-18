package com.bitnet.paulo.mysimpletasks.database;

import java.util.concurrent.TimeUnit;

import org.bson.Document;
import org.bukkit.entity.Player;

import com.bitnet.paulo.mysimpletasks.Main;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoDriverInformation;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.connection.SocketSettings;

public class MongoDBConnection {
	
	private Main plugin;
	public MongoDBConnection(Main plugin) {
		this.plugin = plugin;
	}
	
	private MongoCollection<Document> userColl;
	
	public MongoCollection<Document> getUserColl(){
		return userColl;
	}
	
	public void connect() {
		MongoDriverInformation information = MongoDriverInformation.builder().driverName("sync").build();
		SocketSettings socket = SocketSettings.builder().connectTimeout(1000000, TimeUnit.DAYS).build();
		MongoClientSettings configuration = MongoClientSettings.builder()
				.applyConnectionString(new ConnectionString(Main.configuration.getString("database.mongodb.mongo_uri")))
				.build();
		configuration.getSocketSettings();
		SocketSettings.builder().applySettings(socket).build();
		MongoClient client = MongoClients.create(configuration, information);
		String dataString = Main.configuration.getString("database.mongodb.database");
		String[] split = dataString.split("-");
		userColl = client.getDatabase(split[0]).getCollection(split[1]);
		plugin.getLogger().info("Correctly connected to your MongoDB database!");
	}
	
	public boolean existsUser(Player p) {
		Document doc = new Document("name",p.getName());
		Document find = getUserColl().find(doc).first();
		return find == null ? false : true;
	}
}

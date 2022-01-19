package com.bitnet.paulo.mysimpletasks.manager;

import java.sql.SQLException;

import com.bitnet.paulo.mysimpletasks.Main;
import com.bitnet.paulo.mysimpletasks.database.MongoDBConnection;
import com.bitnet.paulo.mysimpletasks.database.MySQLConnector;

public class DatabaseManager {
	
	public enum DatabaseType{
		MONGO_DB,
		MYSQL
	}
	
	private DatabaseType type;

	public DatabaseType getType() {
		return type;
	}

	public void setType(DatabaseType type) {
		this.type = type;
	}
	
	private Main plugin;
	public DatabaseManager(Main plugin) {
		this.plugin = plugin;
		DatabaseType toSet = DatabaseType.valueOf(Main.configuration.getString("database.type"));
		setType(toSet);
		plugin.getLogger().info("Detected database type: "+getType());
	}
	
	private MySQLConnector mysql;
	private MongoDBConnection mongodb;
	
	public MySQLConnector getMySQL() {
		return mysql;
	}
	
	public MongoDBConnection getMongoDB() {
		return mongodb;
	}
	
	public void start() {
		switch(this.getType()) {
		case MYSQL:
			mysql = new MySQLConnector();
			mysql.setHost(Main.configuration.getString("database.mySQL.host"));
			mysql.setDatabase(Main.configuration.getString("database.mySQL.database"));
			mysql.setUser(Main.configuration.getString("database.mySQL.username"));
			mysql.setPassword(Main.configuration.getString("database.mySQL.password"));
			mysql.setPort(Main.configuration.getInt("database.mySQL.port"));
			mysql.setSSL(Main.configuration.getString("database.mySQL.useSSL"));
			mysql.config();
			try {
				mysql.setConnection();
				plugin.getLogger().info("Correctly connected to your MySQL database");
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			break;
		case MONGO_DB:
			mongodb = new MongoDBConnection(plugin);
			mongodb.connect();
			plugin.getLogger().info("Correctly connected to your MongoDB database");
			break;
		}
	}
	
	public void stop() {
		switch(this.getType()) {
		case MYSQL:
			try {
				mysql.disconnect();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			break;
		case MONGO_DB:
			mongodb.disconnect();
			break;
		}
	}
}

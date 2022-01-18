package com.bitnet.paulo.mysimpletasks.manager;

import java.lang.reflect.InvocationTargetException;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;

import org.bukkit.plugin.Plugin;

import com.bitnet.paulo.mysimpletasks.Main;
import com.bitnet.paulo.mysimpletasks.task.CustomTask;
import com.bitnet.paulo.mysimpletasks.task.list.PlayerKillTask;
import com.bitnet.paulo.mysimpletasks.task.list.PlayerMinerTask;

public class TaskManager {
	
	private Main plugin;
	public TaskManager(Main plugin) {
		this.plugin = plugin;
		plugin.getLogger().info("Loading task manager class...");
		start();
	}
	
	@SuppressWarnings("unchecked")
	protected void start() {
		addTaskToList(PlayerKillTask.class, PlayerMinerTask.class);
		for(CustomTask task : Main.getAvailableTasks()) {
			task.start();
			plugin.getLogger().info("Loading task: "+task.getId()+"-"+task.getName()+" with goal: "+task.getGoal()+", and type: "+task.getTaskType().toString());
			switch(plugin.getDatabaseManager().getType()) {
			case MYSQL:
				try {
					PreparedStatement stmt = plugin.getDatabaseManager().getMySQL().getConnection().prepareStatement("CREATE TABLE IF NOT EXISTS "+task.getName()+"(player varchar(32), points int, isCompleted boolean)");
					stmt.executeUpdate();
					stmt.close();
					plugin.getLogger().info("Table in database created for: "+task.getName());
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				break;
			case MONGO_DB:
			default:
				break;
			}
		}
	}
	
	public void addTaskToList(@SuppressWarnings("unchecked") Class<? extends CustomTask>... classTask) {
		Arrays.stream(classTask).forEach(t -> {
			try {
				CustomTask task = t.getConstructor(Plugin.class).newInstance(plugin);
				Main.getAvailableTasks().add(task);
			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
					| InvocationTargetException | NoSuchMethodException | SecurityException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		});
	}
	
	public CustomTask getTaskByName(String name) {
		for(CustomTask task : Main.getAvailableTasks()) {
			return task.getName().equalsIgnoreCase(name) ? task : null;
		}
		return null;
	}
}

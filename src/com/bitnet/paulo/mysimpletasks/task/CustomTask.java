package com.bitnet.paulo.mysimpletasks.task;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import com.bitnet.paulo.mysimpletasks.Main;
import com.bitnet.paulo.mysimpletasks.utils.XSound;
import com.connorlinfoot.titleapi.TitleAPI;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;

public abstract class CustomTask {
	
	public enum TaskType{
		PLAYER_BUILDER,
		PLAYER_KILL,
		PLAYER_FARMER
	}
	
	public enum DatabaseType{
		MONGODB,
		MYSQL
	}
	
	private String id;
	private String name;
	private int goal;
	private boolean isActive;
	private TaskType taskType;
	private List<Player> taskCompleted_list;
	
	public List<Player> getTaskCompleted_list() {
		return taskCompleted_list;
	}
	
	private ConcurrentHashMap<Player, String[]> dataRam;
	
	public ConcurrentHashMap<Player, String[]> getDataRam(){
		return dataRam;
	}

	private Plugin plugin;
	
	public CustomTask(Plugin plugin, String id, String name, int goal, boolean isActive, TaskType type) {
		this.plugin = plugin;
		this.id = id;
		this.name = name;
		this.goal = goal;
		this.isActive = isActive;
		this.taskType = type;
		this.taskCompleted_list = new ArrayList<>();
		dataRam = new ConcurrentHashMap<>();
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getGoal() {
		return goal;
	}

	public void setGoal(int goal) {
		this.goal = goal;
	}
	
	public boolean isActive() {
		return isActive;
	}

	public void setActive(boolean isActive) {
		this.isActive = isActive;
	}

	public TaskType getTaskType() {
		return taskType;
	}

	public void setTaskType(TaskType taskType) {
		this.taskType = taskType;
	}

	public abstract void start();
	public abstract void upload(Player p);
	
	public Integer getPlayerPoints(Player p) {
		switch(Main.getInstance().getDatabaseManager().getType()) {
		case MONGO_DB:
			if(Main.getInstance().getDatabaseManager().getMongoDB().existsUser(p)) {
				Document doc = new Document("name",p.getName());
				Document data = Main.getInstance().getDatabaseManager().getMongoDB().getUserColl().find(doc).first();
				return data.getInteger(this.getName());
			}else {
				return 0;
			}
		case MYSQL:
			try {
				if(Main.getInstance().getDatabaseManager().getMySQL().existsUser(p, getName())) {
					try(PreparedStatement stmt = Main.getInstance().getDatabaseManager().getMySQL().getConnection().prepareStatement("SELECT points FROM "+this.getName()+" WHERE player=?")){
						stmt.setString(1, p.getName());
						try(ResultSet st = stmt.executeQuery()){
							if(st.next()) {
								return st.getInt("points");
							}
						}
					}
				}else {
					return 0;
				}
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return 0;
	}
	
	@SuppressWarnings("deprecation")
	public void savePeriodically() {
		Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, new BukkitRunnable() {

			@Override
			public void run() {
				// TODO Auto-generated method stub
				for(Player p : Bukkit.getOnlinePlayers()) {
					if(getDataRam().containsKey(p)) {
						String[] values = getDataRam().get(p);
						String[] newData = new String[2];
						boolean check = false;
						switch(Main.getInstance().getDatabaseManager().getType()) {
						case MONGO_DB:
							if(Main.getInstance().getDatabaseManager().getMongoDB().existsUser(p)) {
								Integer value = Integer.parseInt(values[0]) + getPlayerPoints(p);
								Main.getInstance().getDatabaseManager().getMongoDB().getUserColl().updateOne(Filters.eq("name",p.getName()), Updates.set(values[1], value));
								plugin.getLogger().info("All data of all players correctly saved!");
							}else {
								Document doc = new Document("name",p.getName())
										.append(values[1], Integer.parseInt(values[0]))
										.append(getName()+"_isCompleted", "false");
								Main.getInstance().getDatabaseManager().getMongoDB().getUserColl().insertOne(doc);
							}
							Document doc = new Document("name",p.getName());
							Document data = Main.getInstance().getDatabaseManager().getMongoDB().getUserColl().find(doc).first();
							check = Boolean.getBoolean(data.getString(getName()+"_isCompleted"));
							if(getPlayerPoints(p) >= getGoal() && (!(check))) {
								if(!getTaskCompleted_list().contains(p)) {
									getTaskCompleted_list().add(p);
									TitleAPI.sendTitle(p, 0, 30, 20, "&a&l"+getName(), "&e&oTask completed!");
									p.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7[&aMySimpleTasks&7] &8> &fYou just completed the &a"+getName()+" &ftask, please claim your reward"));
									XSound.play(p, "ENTITY_PLAYER_LEVELUP");
								}
							}
							getDataRam().remove(p);
							newData[0] = String.valueOf(0);
							newData[1] = getName();
							getDataRam().put(p, newData);
							break;
						case MYSQL:
							try {
								if(Main.getInstance().getDatabaseManager().getMySQL().existsUser(p, getName())) {
									try(PreparedStatement stmt = Main.getInstance().getDatabaseManager().getMySQL().getConnection().prepareStatement("UPDATE "+values[1]+" SET points=? WHERE player=?")){
										stmt.setInt(1, Integer.parseInt(values[0]) + getPlayerPoints(p));
										stmt.setString(2, p.getName());
										stmt.executeUpdate();
										plugin.getLogger().info("All data of all players correctly saved!");
									}
								}else {
									PreparedStatement stmt = Main.getInstance().getDatabaseManager().getMySQL().getConnection().prepareStatement("INSERT INTO "+values[1]+"(player, points, isCompleted) VALUES (?,?,?)");
									stmt.setString(1, p.getName());
									stmt.setInt(2, Integer.parseInt(values[0]));
									stmt.setBoolean(3, false);
									stmt.executeUpdate();
									stmt.close();
								}
								try(PreparedStatement stmt = Main.getInstance().getDatabaseManager().getMySQL().getConnection().prepareStatement("SELECT isCompleted FROM "+getName()+" WHERE player=?")){
									stmt.setString(1, p.getName());
									ResultSet set = stmt.executeQuery();
									if(set.next()) {
										check = set.getBoolean("isCompleted");
									}
								} catch (SQLException e1) {
									// TODO Auto-generated catch block
									e1.printStackTrace();
								}
								if(getPlayerPoints(p) >= getGoal() && (!(check))) {
									if(!getTaskCompleted_list().contains(p)) {
										TitleAPI.sendTitle(p, 0, 30, 20, "&a&l"+getName(), "&e&oTask completed!");
										getTaskCompleted_list().add(p);
										p.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7[&aMySimpleTasks&7] &8> &fYou just completed the &a"+getName()+" &ftask, please claim your reward"));
										XSound.play(p, "ENTITY_PLAYER_LEVELUP");
									}
								}
							} catch (SQLException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							} finally {
								getDataRam().remove(p);
								newData[0] = String.valueOf(0);
								newData[1] = getName();
								getDataRam().put(p, newData);
							}
							break;
						}
					}
				}
			}
			
		}, 0L, 1200L);
	}
	
	public boolean isTaskActive() {
		return Main.getActiveTasks().contains(this) ? true : false;
	}
	
	public void saveTaskConfig() {
		if(!Main.configuration.contains("tasks."+this.getName())) {
			Main.configuration.set("tasks."+this.getName()+".goal", this.getGoal());
			Main.configuration.set("tasks."+this.getName()+".type", this.getTaskType().toString());
			Main.configuration.set("tasks."+this.getName()+".id", this.getId());
			Main.configuration.set("tasks."+this.getName()+".rewardAction", "GIVE_ITEM|DIAMOND|3");
			Main.configuration.set("tasks."+this.getName()+".isActive", this.isActive());
			Main.configuration.set("tasks."+this.getName()+".item.material", "PAPER");
			Main.configuration.set("tasks."+this.getName()+".item.name", "&e"+this.getName());
			Main.configuration.set("tasks."+this.getName()+".item.lore", Arrays.asList("Your lore","put it here"));
			Main.configuration.set("tasks."+this.getName()+".item.slot", 9);
			try {
				Main.configuration.save(Main.configyml);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}else {
			plugin.getLogger().info("Loaded tasks: "+this.getName()+"-"+this.getId());
		}
	}
}

package com.bitnet.paulo.mysimpletasks.task.list;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

import com.bitnet.paulo.mysimpletasks.Main;
import com.bitnet.paulo.mysimpletasks.task.CustomTask;
import com.bitnet.paulo.mysimpletasks.utils.XSound;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;

public class PlayerKillTask extends CustomTask implements Listener {

	public Main plugin;
	public PlayerKillTask(Plugin plugin) {
		super(plugin, "player_kill", "playerkill", 80, true, TaskType.PLAYER_KILL);
		this.plugin = (Main) plugin;
	}

	@Override
	public void start() {
		// TODO Auto-generated method stub
		this.saveTaskConfig();
		Bukkit.getPluginManager().registerEvents(this, plugin);
		this.savePeriodically();
	}

	@Override
	public void upload(Player p) {
		// TODO Auto-generated method stub
		if(getDataRam().containsKey(p)) {
			String[] data = getDataRam().get(p);
			switch(plugin.getDatabaseManager().getType()) {
			case MONGO_DB:
				if(plugin.getDatabaseManager().getMongoDB().existsUser(p)) {
					Integer value = Integer.parseInt(data[0]) + this.getPlayerPoints(p);
					plugin.getDatabaseManager().getMongoDB().getUserColl().updateOne(Filters.eq("name",p.getName()), Updates.set(this.getName(), value));
				}else {
					Document doc = new Document("name",p.getName())
							.append(this.getName(), Integer.parseInt(data[0]))
							.append(this.getName()+"_isCompleted", "false");
					plugin.getDatabaseManager().getMongoDB().getUserColl().insertOne(doc);
				}
				break;
			case MYSQL:
				try {
					if(plugin.getDatabaseManager().getMySQL().existsUser(p, getName())) {
						PreparedStatement stmt = plugin.getDatabaseManager().getMySQL().getConnection().prepareStatement("UPDATE "+this.getName()+" SET points=? WHERE player=?");
						Integer value = Integer.parseInt(data[0]) + this.getPlayerPoints(p);
						stmt.setInt(1, value);
						stmt.setString(2, p.getName());
						stmt.executeUpdate();
						stmt.close();
					}else {
						PreparedStatement stmt = plugin.getDatabaseManager().getMySQL().getConnection().prepareStatement("INSERT INTO "+this.getName()+"(player, points, isCompleted) VALUES (?,?,?)");
						stmt.setString(1, p.getName());
						stmt.setInt(2, Integer.parseInt(data[0]));
						stmt.setBoolean(3, false);
						stmt.executeUpdate();
						stmt.close();
					}
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				break;
			}
		}
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onKill(EntityDeathEvent e) {
		if(this.isTaskActive()) {
			String[] data = new String[2];
			if(this.getTaskType().equals(TaskType.PLAYER_KILL)) {
				if(e.getEntity().getType().equals(EntityType.ZOMBIE) || e.getEntity().getType().equals(EntityType.CREEPER) || e.getEntity().getType().equals(EntityType.SKELETON) || e.getEntity().getType().equals(EntityType.ENDERMAN) || 
						e.getEntity().getType().equals(EntityType.WITCH) || e.getEntity().getType().equals(EntityType.BLAZE) || e.getEntity().getType().equals(EntityType.SPIDER) || e.getEntity().getType().equals(EntityType.CAVE_SPIDER) ||
						e.getEntity().getType().equals(EntityType.PIG_ZOMBIE) || e.getEntity().getType().equals(EntityType.GHAST)) {
					if(e.getEntity().getKiller() instanceof Player) {
						Player p = e.getEntity().getKiller();
						if(getDataRam().containsKey(e.getEntity().getKiller())) {
							Integer previous_data = Integer.valueOf(getDataRam().get(p)[0]);
							if(e.getEntity().getType().equals(EntityType.WITHER) || e.getEntity().getType().equals(EntityType.ENDER_DRAGON)) {
								Integer newValue = previous_data + 10;
								getDataRam().get(p)[0] = String.valueOf(newValue);
							}
							Integer newValue = previous_data + 1;
							getDataRam().get(p)[0] = String.valueOf(newValue);
							getDataRam().get(p)[1] = this.getName();
							data[0] = getDataRam().get(p)[0];
							data[1] = getDataRam().get(p)[1];
							getDataRam().remove(p);
							getDataRam().put(p, data);
							if(Integer.parseInt(getDataRam().get(p)[0]) == this.getGoal() && (!(this.getPlayerPoints(p) == this.getGoal()))) {
								if(!this.getTaskCompleted_list().contains(p)) {
									this.getTaskCompleted_list().add(p);
									p.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7[&aMySimpleTasks&7] &8> &fYou just completed the &a"+this.getName()+" &ftask, please claim your reward"));
									XSound.play(p, "ENTITY_PLAYER_LEVELUP");
								}
							}
						}else {
							Integer value = 1;
							data[0] = String.valueOf(value);
							data[1] = this.getName();
							getDataRam().put(p, data);
						}
					}
				}
			}
		}
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void playerLeave(PlayerQuitEvent e) {
		Player p = e.getPlayer();
		this.upload(p);
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void verifyRewardList(PlayerJoinEvent e) {
		Player p = e.getPlayer();
		boolean check = false;
		switch(plugin.getDatabaseManager().getType()) {
		case MYSQL:
			try {
				if(plugin.getDatabaseManager().getMySQL().existsUser(p, this.getName())) {
					try(PreparedStatement stmt = plugin.getDatabaseManager().getMySQL().getConnection().prepareStatement("SELECT isCompleted FROM "+this.getName()+" WHERE player=?")){
						stmt.setString(1, p.getName());
						ResultSet set = stmt.executeQuery();
						if(set.next()) {
							check = set.getBoolean("isCompleted");
						}
					} catch (SQLException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				}
			} catch (SQLException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			break;
		case MONGO_DB:
			if(plugin.getDatabaseManager().getMongoDB().existsUser(p)) {
				Document doc = new Document("name",p.getName());
				Document data = plugin.getDatabaseManager().getMongoDB().getUserColl().find(doc).first();
				check = Boolean.getBoolean(data.getString(this.getName()+"_isCompleted"));
				break;
			}
		}
		if(this.getPlayerPoints(p) >= 80 && (!(check))) {
			if(!this.getTaskCompleted_list().contains(p)) {
				this.getTaskCompleted_list().add(p);
				p.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7[&aMySimpleTasks&7] &8> &fYou just completed the &a"+this.getName()+" &ftask, please claim your reward"));
				XSound.play(p, "ENTITY_PLAYER_LEVELUP");
			}
		}
	}
}

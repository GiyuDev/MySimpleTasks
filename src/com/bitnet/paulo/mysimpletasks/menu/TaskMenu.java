package com.bitnet.paulo.mysimpletasks.menu;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.bitnet.paulo.mysimpletasks.Main;
import com.bitnet.paulo.mysimpletasks.task.CustomTask;
import com.connorlinfoot.titleapi.TitleAPI;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;

import me.clip.placeholderapi.PlaceholderAPI;

public class TaskMenu implements Listener {
	
	private Inventory inv;
	
	private Inventory getInv() {
		return this.inv;
	}
	
	private Main plugin;
	public TaskMenu(Main plugin) {
		this.plugin = plugin;
	}
	
	@SuppressWarnings("deprecation")
	public void open(Player p) {
		this.inv = Bukkit.createInventory(null, Main.configuration.getInt("menu_properties.size"), ChatColor.translateAlternateColorCodes('&', Main.configuration.getString("menu_properties.title")));
		if(getInv() != null) {
			if(p.isOnline()) {
				for(String key : Main.configuration.getConfigurationSection("tasks").getKeys(false)) {
					ItemStack item = new ItemStack(Material.getMaterial(Main.configuration.getString("tasks."+key+".item.material")));
					ItemMeta meta = item.getItemMeta();
					String name = Main.configuration.getString("tasks."+key+".item.name");
					name = PlaceholderAPI.setPlaceholders(p, name);
					meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
					List<String> lore = Main.configuration.getStringList("tasks."+key+".item.lore");
					List<String> replaceLore = new ArrayList<>();
					lore.forEach((l)->{
						l = PlaceholderAPI.setPlaceholders(p, l);
						replaceLore.add(ChatColor.translateAlternateColorCodes('&', l));
					});
					meta.setLore(replaceLore);
					meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
					item.setItemMeta(meta);
					getInv().setItem(Main.configuration.getInt("tasks."+key+".item.slot"), item);
				}
				ItemStack close = new ItemStack(Material.getMaterial(Main.configuration.getString("menu_properties.close_item.material")));
				ItemMeta meta = close.getItemMeta();
				meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', Main.configuration.getString("menu_properties.close_item.name")));
				List<String> lore = Main.configuration.getStringList("menu_properties.close_item.lore");
				List<String> replaceLore = new ArrayList<>();
				lore.forEach((l)->{
					l = PlaceholderAPI.setPlaceholders(p, l);
					replaceLore.add(ChatColor.translateAlternateColorCodes('&', l));
				});
				meta.setLore(replaceLore);
				meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
				close.setItemMeta(meta);
				getInv().setItem(Main.configuration.getInt("menu_properties.close_item.slot"), close);
				if(Main.configuration.getBoolean("menu_properties.decorative_item.use")) {
					ItemStack decorative = new ItemStack(Material.valueOf(Main.configuration.getString("menu_properties.decorative_item.material").toUpperCase()), 1, (short)Main.configuration.getInt("menu_properties.decorative_item.data"));
					ItemMeta meta_decorative = decorative.getItemMeta();
					meta_decorative.setDisplayName(ChatColor.translateAlternateColorCodes('&', Main.configuration.getString("menu_properties.decorative_item.name")));
					List<String> lore_d = Main.configuration.getStringList("menu_properties.decorative_item.lore");
					List<String> replaceLore_d = new ArrayList<>();
					lore_d.forEach((l)->{
						l = PlaceholderAPI.setPlaceholders(p, l);
						replaceLore_d.add(ChatColor.translateAlternateColorCodes('&', l));
					});
					meta_decorative.setLore(replaceLore_d);
					meta_decorative.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
					decorative.setItemMeta(meta_decorative);
					Main.configuration.getIntegerList("menu_properties.decorative_item.slot_list").forEach(slot->{
						getInv().setItem(slot, decorative);
					});
					if(Main.configuration.getBoolean("menu_properties.decorative_item.use_all_empty_slots")) {
						for(int i = 0; i<getInv().getSize(); i++) {
							if(getInv().getItem(i) == null) {
								getInv().setItem(i, decorative);
							}else if(getInv().getItem(i).getType().equals(Material.AIR)) {
								getInv().setItem(i, decorative);
							}
						}
					}
				}
				p.openInventory(getInv());
			}
		}
	}
	
	@EventHandler
	public void onClick(InventoryClickEvent e) {
		if(e.getView().getTitle().equalsIgnoreCase(ChatColor.translateAlternateColorCodes('&', Main.configuration.getString("menu_properties.title")))) {
			e.setCancelled(true);
			Player p = (Player) e.getWhoClicked();
			ItemStack item = e.getCurrentItem();
			if(item == null || item.getType().equals(Material.AIR)) return;
			for(CustomTask task : Main.getAvailableTasks()) {
				switch(plugin.getDatabaseManager().getType()) {
				case MYSQL:
					try {
						if(item.getItemMeta().getDisplayName().contains(task.getName())) {
							if(plugin.getDatabaseManager().getMySQL().existsUser(p, task.getName())) {
								boolean check = false;
								try(PreparedStatement stmt = plugin.getDatabaseManager().getMySQL().getConnection().prepareStatement("SELECT isCompleted FROM "+task.getName()+" WHERE player=?")){
									stmt.setString(1, p.getName());
									ResultSet set = stmt.executeQuery();
									if(set.next()) {
										check = set.getBoolean("isCompleted");
									}
								} catch (SQLException e1) {
									// TODO Auto-generated catch block
									e1.printStackTrace();
								}
								if(task.getTaskCompleted_list().contains(p) && (!(check))) {
									p.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7[&aMySimpleTasks&7] &8> &fCongratulations! the reward for the task &a"+task.getName()+" &fwas correctly claimed!"));
									TitleAPI.sendTitle(p, 0, 30, 20, "&a&l"+task.getName(), "&e&oTask completed!");
									ItemStack reward = new ItemStack(Material.DIAMOND);
									reward.setAmount(3);
									p.getInventory().addItem(reward);
									task.getTaskCompleted_list().remove(p);
									try(PreparedStatement stmt = plugin.getDatabaseManager().getMySQL().getConnection().prepareStatement("UPDATE "+task.getName()+" SET isCompleted=? WHERE player=?")){
										stmt.setBoolean(1, true);
										stmt.setString(2, p.getName());
										stmt.executeUpdate();
									} catch (SQLException e1) {
										// TODO Auto-generated catch block
										e1.printStackTrace();
									}
								}else {
									p.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7[&aMySimpleTasks&7] &8> &fYou ain't complete the task &a"+task.getName()+"&f, or you already claim the reward"));
								}
								p.closeInventory();
							}
						}
					} catch (SQLException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					break;
				case MONGO_DB:
					if(item.getItemMeta().getDisplayName().contains(task.getName())) {
						boolean check = false;
						Document doc = new Document("name",p.getName());
						Document data = plugin.getDatabaseManager().getMongoDB().getUserColl().find(doc).first();
						check = Boolean.getBoolean(data.getString(task.getName()+"_isCompleted"));
						if(task.getTaskCompleted_list().contains(p) && (!(check))) {
							p.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7[&aMySimpleTasks&7] &8> &fCongratulations the reward for the task &a"+task.getName()+" &fwas correctly claimed!"));
							TitleAPI.sendTitle(p, 0, 30, 20, "&a&l"+task.getName(), "&e&oTask completed!");
							ItemStack reward = new ItemStack(Material.DIAMOND);
							reward.setAmount(3);
							p.getInventory().addItem(reward);
							task.getTaskCompleted_list().remove(p);
							plugin.getDatabaseManager().getMongoDB().getUserColl().updateOne(Filters.eq("name",p.getName()), Updates.set(task.getName()+"_isCompleted", "true"));
						}else {
							p.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7[&aMySimpleTasks&7] &8> &fYou ain't complete the task &a"+task.getName()+"&f, or you already claim the reward"));
						}
						p.closeInventory();
					}
					break;
				}
			}
			if(item.getItemMeta().getDisplayName().equals(ChatColor.translateAlternateColorCodes('&', Main.configuration.getString("menu_properties.close_item.name")))) {
				p.closeInventory();
			}
		}
	}
}

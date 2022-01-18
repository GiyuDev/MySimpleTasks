package com.bitnet.paulo.mysimpletasks.commands;

import java.io.File;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import com.bitnet.paulo.mysimpletasks.Main;

import me.clip.placeholderapi.PlaceholderAPI;

public class AdminCMD implements CommandExecutor {
	
	private Main plugin;
	public AdminCMD(Main plugin) {
		this.plugin = plugin;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(!(sender instanceof Player)) {
			plugin.getLogger().warning("You can't execute this command in console!");
			return false;
		}else {
			Player p = (Player) sender;
			if(args.length <= 0) {
				p.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7[&aMySimpleTasks&7] &8> &fPlugin made by: &6Giyu"));
			}else if(args.length >= 1) {
				switch(args[0]) {
				case "reload":
					Main.configyml = new File(plugin.getDataFolder(), "config.yml");
                    Main.configuration = YamlConfiguration.loadConfiguration(Main.configyml);
                    p.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7[&aMySimpleTasks&7] &8> &fPlugin files correctly reloaded!"));
                    break;
				case "mypoints":
					Main.getAvailableTasks().forEach(task ->{
						if(task.getName().equals(args[1])) {
							String msg = "&7[&aMySimpleTasks&7] &8> &fYour current points for &c"+task.getName()+" &ftask are: &a%mst_"+task.getName()+"_player_points%";
							msg = PlaceholderAPI.setPlaceholders(p, msg);
							p.sendMessage(ChatColor.translateAlternateColorCodes('&', msg));
						}
					});
					break;
				case "menu":
					plugin.getTaskMenu().open(p);
					break;
					default:
						p.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7[&aMySimpleTasks&7] &8> &fCommand wasn't found!"));
						break;
				}
			}
		}
		return true;
	}

}

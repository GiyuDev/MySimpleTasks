package com.bitnet.paulo.mysimpletasks.utils;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.bitnet.paulo.mysimpletasks.Main;
import com.bitnet.paulo.mysimpletasks.task.CustomTask;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;

public class PAPIHook extends PlaceholderExpansion {

	@Override
	public @NotNull String getIdentifier() {
		// TODO Auto-generated method stub
		return "mst";
	}

	@Override
	public @NotNull String getAuthor() {
		// TODO Auto-generated method stub
		return "Giyu";
	}

	@Override
	public @NotNull String getVersion() {
		// TODO Auto-generated method stub
		return "1.0";
	}
	
	@Override
	public @Nullable String onPlaceholderRequest(Player player, @NotNull String params) {
		for(CustomTask task : Main.getAvailableTasks()) {
			if(params.equalsIgnoreCase(task.getName()+"_player_points")) {
				return String.valueOf(task.getPlayerPoints(player));
			}
			if(params.equalsIgnoreCase(task.getName()+"_goal")) {
				return String.valueOf(task.getGoal());
			}
		}
		return null;
	}
}

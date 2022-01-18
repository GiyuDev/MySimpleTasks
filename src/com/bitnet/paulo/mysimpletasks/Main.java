package com.bitnet.paulo.mysimpletasks;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import com.bitnet.paulo.mysimpletasks.commands.AdminCMD;
import com.bitnet.paulo.mysimpletasks.manager.DatabaseManager;
import com.bitnet.paulo.mysimpletasks.manager.TaskManager;
import com.bitnet.paulo.mysimpletasks.menu.TaskMenu;
import com.bitnet.paulo.mysimpletasks.task.CustomTask;
import com.bitnet.paulo.mysimpletasks.utils.PAPIHook;

public class Main extends JavaPlugin {
	
	public String rutaConfig;
    public static File configyml;
    public static YamlConfiguration configuration;
	
	public static ArrayList<CustomTask> availableTasks;
	
	public static ArrayList<CustomTask> getAvailableTasks(){
		return availableTasks;
	}
	
	public static List<CustomTask> getActiveTasks(){
		return availableTasks.stream().filter(t -> t.isActive()).collect(Collectors.toList());
	}
	
	private TaskManager taskManager;
	public TaskManager getTaskManager() {
		return taskManager;
	}
	
	private DatabaseManager databaseManager;
	public DatabaseManager getDatabaseManager() {
		return databaseManager;
	}
	
	public static Main instance;
	public static Main getInstance() {
		return instance;
	}
	
	private static boolean isPapiHook = false;
	public static boolean checkPapiHook() {
		return isPapiHook;
	}
	
	private PAPIHook papiHook;
	
	private TaskMenu taskMenu;
	public TaskMenu getTaskMenu() {
		return taskMenu;
	}
	
	@Override
	public void onEnable() {
		instance = this;
		this.getLogger().info("Loading the plugin");
		configyml = new File(this.getDataFolder(), "config.yml");
        registerConfiguration();
        configuration = YamlConfiguration.loadConfiguration(configyml);
		availableTasks = new ArrayList<>();
		databaseManager = new DatabaseManager(this);
		databaseManager.start();
		Bukkit.getScheduler().runTaskLater(this, ()->{
			taskManager = new TaskManager(this);
			checkHooks();
			taskMenu = new TaskMenu(this);
		}, 5L);
		commands();
		Bukkit.getPluginManager().registerEvents((Listener)new TaskMenu(this), this);
		this.getLogger().info("Plugin correctly enabled!");
	}
	
	public void registerConfiguration() {
        rutaConfig = configyml.getAbsolutePath();
        if (!(configyml.exists())) {
            setConfigurationDefaults();
        }
    }

    public void setConfigurationDefaults() {
        this.saveResource("config.yml", true);
    }
    
    private void checkHooks() {
    	if(Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null && Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
    		isPapiHook = true;
    		papiHook = new PAPIHook();
    		papiHook.register();
    		this.getLogger().info("PlaceholderAPI was correctly hooked to MySimpleTasks");
    	}else {
    		this.getLogger().warning("PlaceholderAPI wasn't hooked to MySimpleTasks due to it's disabled");
    	}
    }
    
    private void commands() {
    	this.getCommand("mysimpletasks").setExecutor(new AdminCMD(this));
    }
}

package hhitt.fancyglow;

import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.dvs.Pattern;
import dev.dejvokep.boostedyaml.dvs.segment.Segment;
import dev.dejvokep.boostedyaml.settings.dumper.DumperSettings;
import dev.dejvokep.boostedyaml.settings.general.GeneralSettings;
import dev.dejvokep.boostedyaml.settings.loader.LoaderSettings;
import dev.dejvokep.boostedyaml.settings.updater.UpdaterSettings;
import hhitt.fancyglow.api.FancyGlowAPI;
import hhitt.fancyglow.api.FancyGlowAPIImpl;
import hhitt.fancyglow.inventory.CreatingInventory;
import hhitt.fancyglow.listeners.*;
import hhitt.fancyglow.managers.CommandLoader;
import hhitt.fancyglow.managers.GlowManager;
import hhitt.fancyglow.managers.PlayerGlowManager;
import hhitt.fancyglow.utils.FancyGlowPlaceholder;
import hhitt.fancyglow.utils.MessageHandler;
import hhitt.fancyglow.utils.MessageUtils;
import hhitt.fancyglow.utils.TabImplementation;
import hhitt.fancyglow.utils.UpdateChecker;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * Main class for FancyGlow.
 * Converted to a standard JavaPlugin (No Zapper dependency).
 */
public final class FancyGlow extends JavaPlugin {

    private static FancyGlowAPI API;
    private final Logger logger = this.getLogger();

    private YamlDocument configuration;
    private MessageHandler messageHandler;

    private GlowManager glowManager;
    private PlayerGlowManager playerGlowManager;

    private CommandLoader commandLoader;
    private CreatingInventory inventory;
    private FancyGlowPlaceholder papiExpansion;

    @Override
    public void onEnable() {
        // Run internal hooks and metrics async to prevent main-thread lag
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            // bStats hook / metrics
            new Metrics(this, 22057);
            
            // Check for plugin updates
            checkUpdates();

            // Attempts to hook onto TAB API (Automatically updates nametags)
            new TabImplementation(this).initialize();
        });

        // Initialize Configuration Manager (BoostedYAML)
        try {
            this.configuration = YamlDocument.create(
                    new File(this.getDataFolder(), "config.yml"),
                    Objects.requireNonNull(getResource("config.yml")),
                    GeneralSettings.DEFAULT,
                    LoaderSettings.builder().setAutoUpdate(true).build(),
                    DumperSettings.DEFAULT,
                    UpdaterSettings.builder().setVersioning(new Pattern(Segment.range(1, Integer.MAX_VALUE)), "config-version").build());
        } catch (IOException e) {
            throw new RuntimeException("Could not create/load config.yml", e);
        }

        this.messageHandler = new MessageHandler(this, configuration);

        // Initialize Core Managers
        this.glowManager = new GlowManager(this);
        this.playerGlowManager = new PlayerGlowManager(this);
        
        // Start background tasks for Flashing and Rainbow effects
        this.glowManager.scheduleFlashingTask();
        this.glowManager.scheduleMulticolorTask();
        
        // Setup GUI Inventory
        this.inventory = new CreatingInventory(this);
        this.inventory.setupContent();

        // Instance API and register it as a Bukkit service
        API = new FancyGlowAPIImpl(this);
        getServer().getServicesManager().register(FancyGlowAPI.class, API, this, ServicePriority.Normal);

        // Register commands and suggestions
        this.commandLoader = new CommandLoader(this);

        // Register Event Listeners
        registerEvents();

        // Hook into PlaceholderAPI
        hookPlaceholderAPI();
        
        this.logger.info("FancyGlow has been enabled successfully!");
    }

    @Override
    public void onDisable() {
        // Unregister PAPI expansion so PlugMan reload gets a fresh instance
        if (this.papiExpansion != null && Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            PlaceholderAPI.unregisterExpansion(this.papiExpansion);
            this.papiExpansion = null;
        }

        // Unregister Commands
        if (this.commandLoader != null) {
            this.commandLoader.unregisterAll();
        }

        // Stop active glow tasks and cancel every remaining scheduled task
        if (this.glowManager != null) {
            this.glowManager.stopFlashingTask();
            this.glowManager.stopMulticolorTask();
        }
        getServer().getScheduler().cancelTasks(this);

        // Reset static API reference so it is not leaked across reloads
        API = null;

        this.logger.info("FancyGlow has been disabled.");
    }

    /**
     * Registers all plugin event listeners.
     */
    public void registerEvents() {
        PluginManager pluginManager = getServer().getPluginManager();

        pluginManager.registerEvents(new MenuClickListener(this), this);
        pluginManager.registerEvents(new HeadClickListener(this), this);
        pluginManager.registerEvents(new PlayerJoinListener(this), this);
        pluginManager.registerEvents(new PlayerQuitListener(this), this);
        pluginManager.registerEvents(new PlayerChangeWorldListener(this), this);
    }

    /**
     * Checks if a newer version of the plugin is available on SpigotMC.
     */
    private void checkUpdates() {
        if (!configuration.getBoolean("Notify_Updates", true)) return;
        
        UpdateChecker.init(this, 116326).requestUpdateCheck().whenComplete((result, exception) -> {
            if (exception != null) {
                this.logger.warning("Failed to check for updates: " + exception.getMessage());
                return;
            }
            
            if (result.requiresUpdate()) {
                this.logger.info("--------------------------------------------------");
                this.logger.info(String.format("There is a new update available! FancyGlow %s", result.getNewestVersion()));
                this.logger.info("Download it at: https://www.spigotmc.org/resources/116326/");
                this.logger.info("--------------------------------------------------");
            }
        });
    }

    private void hookPlaceholderAPI() {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) {
            this.logger.warning("PlaceholderAPI not found! Internal placeholders will not work in other plugins.");
            return;
        }

        this.papiExpansion = new FancyGlowPlaceholder(this);
        this.papiExpansion.register();
    }

    // --- Getters ---

    public static FancyGlowAPI getAPI() {
        return API;
    }

    public YamlDocument getConfiguration() {
        return configuration;
    }

    public MessageHandler getMessageHandler() {
        return messageHandler;
    }

    public GlowManager getGlowManager() {
        return glowManager;
    }

    public PlayerGlowManager getPlayerGlowManager() {
        return playerGlowManager;
    }

    public CreatingInventory getInventory() {
        return inventory;
    }

    public void setInventory(CreatingInventory inventory) {
        this.inventory = inventory;
    }
}
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
import hhitt.fancyglow.utils.UpdateChecker;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.logging.Logger;

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
        Bukkit.getScheduler().runTaskAsynchronously(this, this::checkUpdates);

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

        this.glowManager = new GlowManager(this);
        this.playerGlowManager = new PlayerGlowManager(this);

        this.glowManager.scheduleFlashingTask();
        this.glowManager.scheduleMulticolorTask();

        this.inventory = new CreatingInventory(this);
        this.inventory.setupContent();

        API = new FancyGlowAPIImpl(this);
        getServer().getServicesManager().register(FancyGlowAPI.class, API, this, ServicePriority.Normal);

        this.commandLoader = new CommandLoader(this);

        registerEvents();
        hookPlaceholderAPI();

        this.logger.info("FancyGlow has been enabled successfully!");
    }

    @Override
    public void onDisable() {
        if (this.papiExpansion != null && Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            PlaceholderAPI.unregisterExpansion(this.papiExpansion);
            this.papiExpansion = null;
        }

        if (this.commandLoader != null) {
            this.commandLoader.unregisterAll();
        }

        if (this.glowManager != null) {
            this.glowManager.stopFlashingTask();
            this.glowManager.stopMulticolorTask();
        }
        getServer().getScheduler().cancelTasks(this);

        API = null;

        this.logger.info("FancyGlow has been disabled.");
    }

    public void registerEvents() {
        PluginManager pluginManager = getServer().getPluginManager();

        pluginManager.registerEvents(new MenuClickListener(this), this);
        pluginManager.registerEvents(new HeadClickListener(this), this);
        pluginManager.registerEvents(new PlayerJoinListener(this), this);
        pluginManager.registerEvents(new PlayerQuitListener(this), this);
        pluginManager.registerEvents(new PlayerChangeWorldListener(this), this);
    }

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

package hhitt.fancyglow.listeners;

import dev.dejvokep.boostedyaml.YamlDocument;
import hhitt.fancyglow.FancyGlow;
import hhitt.fancyglow.managers.GlowManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoinListener implements Listener {

    private final GlowManager glowManager;
    private final YamlDocument config;

    public PlayerJoinListener(FancyGlow plugin) {
        this.glowManager = plugin.getGlowManager();
        this.config = plugin.getConfiguration();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!config.getBoolean("Persistent_Mode")) {
            glowManager.removeGlow(event.getPlayer());
        }
    }
}

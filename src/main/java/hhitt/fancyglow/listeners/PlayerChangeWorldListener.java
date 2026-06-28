package hhitt.fancyglow.listeners;

import hhitt.fancyglow.FancyGlow;
import hhitt.fancyglow.managers.GlowManager;
import hhitt.fancyglow.managers.PlayerGlowManager;
import hhitt.fancyglow.utils.MessageHandler;
import hhitt.fancyglow.utils.Messages;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;

public class PlayerChangeWorldListener implements Listener {

    private final GlowManager glowManager;
    private final MessageHandler messageHandler;
    private final PlayerGlowManager playerGlowManager;

    public PlayerChangeWorldListener(FancyGlow plugin) {
        this.glowManager = plugin.getGlowManager();
        this.messageHandler = plugin.getMessageHandler();
        this.playerGlowManager = plugin.getPlayerGlowManager();
    }

    @EventHandler
    public void onPlayerWorldChange(PlayerChangedWorldEvent e) {
        Player player = e.getPlayer();
        if (glowManager.isDeniedWorld(player.getWorld().getName())
                && !playerGlowManager.getPlayerGlowingMode(player).equalsIgnoreCase("NONE")) {
            glowManager.removeGlow(player);
            messageHandler.sendMessage(player, Messages.DISABLED_WORLD);
        }
    }
}

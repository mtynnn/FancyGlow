package hhitt.fancyglow.commands;

import dev.rollczi.litecommands.annotations.command.Command;
import dev.rollczi.litecommands.annotations.context.Context;
import dev.rollczi.litecommands.annotations.description.Description;
import dev.rollczi.litecommands.annotations.execute.Execute;
import dev.rollczi.litecommands.annotations.permission.Permission;
import hhitt.fancyglow.FancyGlow;
import hhitt.fancyglow.inventory.CreatingInventory;
import hhitt.fancyglow.managers.GlowManager;
import hhitt.fancyglow.utils.MessageHandler;
import hhitt.fancyglow.utils.Messages;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.IOException;

@Command(name = "glow", aliases = "fancyglow")
@Description("Main command for FancyGlow")
public class MainCommand {

    private final FancyGlow plugin;
    private final GlowManager glowManager;
    private final MessageHandler messageHandler;

    public MainCommand(FancyGlow plugin) {
        this.plugin = plugin;
        this.glowManager = plugin.getGlowManager();
        this.messageHandler = plugin.getMessageHandler();
    }

    @Execute
    public void command(@Context Player player) {
        // Returns if disabled so player use its own menus.
        if (!plugin.getConfiguration().getBoolean("Open_Glow_Menu")) return;

        // Prevent command usage in target worlds
        if (glowManager.isDeniedWorld(player.getWorld().getName())) {
            messageHandler.sendMessage(player, Messages.DISABLED_WORLD);
            return;
        }

        // Check gui permissions
        if (!player.hasPermission("fancyglow.command.gui")) {
            messageHandler.sendMessage(player, Messages.NO_PERMISSION);
            return;
        }

        // Prepare and open the gui
        plugin.getInventory().openInventory(player);
    }

    @Execute(name = "reload")
    @Permission("fancyglow.command.reload")
    @Description("FancyGlow reload sub-command.")
    public void reloadCommand(@Context CommandSender sender) {
        glowManager.stopFlashingTask();
        glowManager.stopMulticolorTask();

        try {
            plugin.getConfiguration().reload();
        } catch (IOException e) {
            plugin.getLogger().severe("Unexpected exception during configuration-reload: " + e.getMessage());
        } finally {
            // Close the old inventory for any player that has it open, then
            // recreate it so the title (set at Bukkit inventory creation) refreshes.
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (online.getOpenInventory().getTopInventory().getHolder() instanceof CreatingInventory) {
                    online.closeInventory();
                }
            }
            plugin.setInventory(new CreatingInventory(plugin));
            plugin.getInventory().setupContent();

            glowManager.scheduleFlashingTask();
            glowManager.scheduleMulticolorTask();

            messageHandler.sendMessage(sender, Messages.RELOADED);
        }
    }
}

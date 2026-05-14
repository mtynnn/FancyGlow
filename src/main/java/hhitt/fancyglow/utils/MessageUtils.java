package hhitt.fancyglow.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class MessageUtils {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    // Legacy serializer still needed for ItemMeta.setDisplayName / setLore (legacy API)
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.builder()
            .character('§')
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build();

    public static Component parse(String message) {
        return MINI_MESSAGE.deserialize(message);
    }

    public static @NotNull String miniMessageParse(String message) {
        return LEGACY.serialize(parse(message));
    }

    /** Sends a MiniMessage-formatted string to a player using Paper's native Adventure API. */
    public static void miniMessageSender(Player player, String message) {
        player.sendMessage(parse(message));
    }
}

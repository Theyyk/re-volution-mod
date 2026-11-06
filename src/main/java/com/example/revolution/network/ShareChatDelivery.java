package com.example.revolution.network;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.HoverEvent;

/** One server-authoritative standard-chat transport for existing Share actions. */
public final class ShareChatDelivery {
    private static final long MIN_INTERVAL_MS = 850L;
    private static final Map<UUID, Long> LAST_SHARE = new HashMap<>();

    private ShareChatDelivery() {}

    /** Time is supplied for deterministic anti-repeat tests. Called on the server thread. */
    static synchronized boolean accept(UUID playerId, long now) {
        if (playerId == null) return false;
        Long last = LAST_SHARE.get(playerId);
        if (last != null && now >= last && now - last < MIN_INTERVAL_MS) return false;
        if (LAST_SHARE.size() >= 4096) {
            LAST_SHARE.entrySet().removeIf(entry -> now - entry.getValue() > 60000L);
        }
        LAST_SHARE.put(playerId, now);
        return true;
    }

    public static void send(EntityPlayerMP player, String label, String details) {
        if (player == null || player.getServer() == null || label == null || details == null) return;
        if (!accept(player.getUniqueID(), System.currentTimeMillis())) return;
        TextComponentString tag = new TextComponentString("[" + label + "]");
        tag.setStyle(new Style().setColor(TextFormatting.AQUA)
                .setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponentString(details))));
        TextComponentString message = new TextComponentString(player.getName() + " ");
        message.appendSibling(tag);
        player.getServer().getPlayerList().sendMessage(message, false);
    }
}

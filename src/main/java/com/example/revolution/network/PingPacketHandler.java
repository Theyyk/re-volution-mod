package com.example.revolution.network;

import com.example.revolution.lifecycle.ServerLifecycle;
import com.example.revolution.rune.service.RunePurchaseProfiling;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** Forge adapter: accepted requests are executed on the server thread. */
public final class PingPacketHandler implements IMessageHandler<PingPacket, IMessage> {
    @Override public IMessage onMessage(PingPacket message, MessageContext ctx) {
        EntityPlayerMP player = ctx.getServerHandler().player;
        if (player == null || message == null) return null;
        String action = message.getAction();
        if (action != null && (action.equals("buy_card") || action.startsWith("buy_cards:"))) {
            long enqueuedAt = System.nanoTime();
            RunePurchaseProfiling.queued("ordinary");
            player.getServerWorld().addScheduledTask(() -> RunePurchaseProfiling.process(
                    "ordinary", enqueuedAt,
                    () -> ServerLifecycle.services().runeActions.handle(player, action)));
        } else {
            player.getServerWorld().addScheduledTask(() -> ServerLifecycle.services().runeActions.handle(player, action));
        }
        return null;
    }
}

package com.example.revolution.network;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * Filters enhancement packet bursts before delegating accepted traffic to the existing handler.
 */
public final class RateLimitedPingPacketHandler implements IMessageHandler<PingPacket, IMessage> {
    private static final String ENHANCE_PREFIX = "enhance_rune:";
    private static final EnhancementRequestLimiter ENHANCEMENT_LIMITER = new EnhancementRequestLimiter();
    private final PingPacketHandler delegate = new PingPacketHandler();

    public static void reset() { ENHANCEMENT_LIMITER.clear(); }

    @Override
    public IMessage onMessage(PingPacket message, MessageContext ctx) {
        String action = message == null ? null : message.getAction();
        if (action != null && action.startsWith(ENHANCE_PREFIX)) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            if (player == null || !ENHANCEMENT_LIMITER.tryAcquire(player.getUniqueID())) return null;
        }
        return delegate.onMessage(message, ctx);
    }
}

package com.example.revolution.network;

import com.example.revolution.RevolutionMod;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.play.client.CPacketResourcePackStatus;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.relauncher.Side;

/** Local dedicated-server guard for 1.12.2, which has no built-in required-pack policy. */
@Mod.EventBusSubscriber(modid = RevolutionMod.MODID, value = Side.SERVER)
public final class RequiredResourcePack {
    private static final String HANDLER = "revolution_required_resource_pack";
    private static final Map<UUID, Pending> pending = new HashMap<>();
    private static final long TIMEOUT = TimeUnit.SECONDS.toNanos(120);

    private RequiredResourcePack() {}

    @SubscribeEvent public static void connected(FMLNetworkEvent.ServerConnectionFromClientEvent event) {
        if (!Boolean.getBoolean("revolution.resourcePack.required") || !(event.getHandler() instanceof NetHandlerPlayServer)) return;
        final EntityPlayerMP player = ((NetHandlerPlayServer) event.getHandler()).player;
        final MinecraftServer server = player.getServer();
        if (server == null || server.getResourcePackUrl().isEmpty() || !server.getResourcePackHash().matches("[0-9a-fA-F]{40}")) {
            disconnect(player, "На сервере не настроен обязательный ресурспак. Сообщите администратору.");
            return;
        }
        final Pending request = new Pending(player);
        pending.put(player.getUniqueID(), request);
        final Channel channel = player.connection.netManager.channel();
        // Forge fires this before JoinGame and the pack request; queue the guard first.
        channel.eventLoop().execute(() -> {
            try {
                channel.pipeline().addBefore("packet_handler", HANDLER, new ChannelInboundHandlerAdapter() {
                    @Override public void channelRead(ChannelHandlerContext context, Object message) throws Exception {
                        if (message instanceof CPacketResourcePackStatus) {
                            // 1.12.2 exposes no status getter. Use the packet's public wire serializer.
                            ByteBuf data = Unpooled.buffer(5);
                            final CPacketResourcePackStatus.Action action;
                            try {
                                PacketBuffer packet = new PacketBuffer(data);
                                ((CPacketResourcePackStatus) message).writePacketData(packet);
                                action = packet.readEnumValue(CPacketResourcePackStatus.Action.class);
                            } finally { data.release(); }
                            server.addScheduledTask(() -> status(request, action));
                        }
                        super.channelRead(context, message);
                    }
                });
            } catch (RuntimeException failure) {
                server.addScheduledTask(() -> {
                    if (pending.remove(player.getUniqueID(), request)) disconnect(player, "Не удалось проверить обязательный ресурспак. Подключитесь повторно.");
                });
            }
        });
    }

    private static void status(Pending request, CPacketResourcePackStatus.Action action) {
        EntityPlayerMP player = request.player;
        if (pending.get(player.getUniqueID()) != request) return;
        if (action == CPacketResourcePackStatus.Action.SUCCESSFULLY_LOADED) {
            pending.remove(player.getUniqueID());
            removeHandler(player);
        } else if (action == CPacketResourcePackStatus.Action.DECLINED || action == CPacketResourcePackStatus.Action.FAILED_DOWNLOAD) {
            pending.remove(player.getUniqueID());
            disconnect(player, "Для игры нужен ресурспак RE:volution. Разрешите серверные наборы ресурсов и подключитесь повторно.");
        }
    }

    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Iterator<Pending> iterator = pending.values().iterator();
        while (iterator.hasNext()) {
            Pending request = iterator.next();
            if (System.nanoTime() - request.started >= TIMEOUT) {
                iterator.remove();
                disconnect(request.player, "Истекло время загрузки обязательного ресурспака. Проверьте загрузку и подключитесь повторно.");
            }
        }
    }

    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        pending.remove(event.player.getUniqueID());
        if (event.player instanceof EntityPlayerMP) removeHandler((EntityPlayerMP) event.player);
    }

    private static void removeHandler(EntityPlayerMP player) {
        Channel channel = player.connection.netManager.channel();
        channel.eventLoop().execute(() -> {
            if (channel.pipeline().get(HANDLER) != null) channel.pipeline().remove(HANDLER);
        });
    }

    private static void disconnect(EntityPlayerMP player, String reason) {
        removeHandler(player);
        player.connection.disconnect(new TextComponentString(reason));
    }

    private static final class Pending {
        final EntityPlayerMP player;
        final long started = System.nanoTime();
        Pending(EntityPlayerMP player) { this.player = player; }
    }
}

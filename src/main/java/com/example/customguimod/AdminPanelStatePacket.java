package com.example.customguimod;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.List;

public final class AdminPanelStatePacket implements IMessage {
    private List<String> players = new ArrayList<>();
    private List<String> resources = new ArrayList<>();
    private List<String> mobs = new ArrayList<>();

    public AdminPanelStatePacket() {}

    public AdminPanelStatePacket(List<String> players, List<String> resources, List<String> mobs) {
        this.players = new ArrayList<>(players);
        this.resources = new ArrayList<>(resources);
        this.mobs = new ArrayList<>(mobs);
    }

    public List<String> getPlayers() { return players; }
    public List<String> getResources() { return resources; }
    public List<String> getMobs() { return mobs; }

    @Override
    public void fromBytes(ByteBuf buf) {
        players = readList(buf);
        resources = readList(buf);
        mobs = readList(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        writeList(buf, players);
        writeList(buf, resources);
        writeList(buf, mobs);
    }

    private static void writeList(ByteBuf buf, List<String> values) {
        buf.writeInt(values.size());
        for (String value : values) ByteBufUtils.writeUTF8String(buf, value == null ? "" : value);
    }

    private static List<String> readList(ByteBuf buf) {
        int size = Math.max(0, Math.min(1024, buf.readInt()));
        List<String> result = new ArrayList<>(size);
        for (int i = 0; i < size; i++) result.add(ByteBufUtils.readUTF8String(buf));
        return result;
    }

    @SideOnly(Side.CLIENT)
    public static final class Handler implements IMessageHandler<AdminPanelStatePacket, IMessage> {
        @Override
        public IMessage onMessage(AdminPanelStatePacket message, MessageContext ctx) {
            Minecraft.getMinecraft().addScheduledTask(() -> {
                if (Minecraft.getMinecraft().currentScreen instanceof AdminPanelScreen) {
                    ((AdminPanelScreen) Minecraft.getMinecraft().currentScreen).applyState(message);
                } else {
                    Minecraft.getMinecraft().displayGuiScreen(new AdminPanelScreen(message));
                }
            });
            return null;
        }
    }
}

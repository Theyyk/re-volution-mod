package com.example.customguimod.admin;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

import java.util.ArrayList;
import java.util.List;

public final class AdminPanelStatePacket implements IMessage {
    private List<String> players = new ArrayList<>();
    private List<String> resources = new ArrayList<>();
    private List<String> mobs = new ArrayList<>();
    private List<String> balances = new ArrayList<>();

    public AdminPanelStatePacket() {}

    public AdminPanelStatePacket(List<String> players, List<String> resources,
                                 List<String> mobs, List<String> balances) {
        this.players = new ArrayList<>(players);
        this.resources = new ArrayList<>(resources);
        this.mobs = new ArrayList<>(mobs);
        this.balances = new ArrayList<>(balances);
    }

    public List<String> getPlayers() { return players; }
    public List<String> getResources() { return resources; }
    public List<String> getMobs() { return mobs; }
    public List<String> getBalances() { return balances; }

    @Override
    public void fromBytes(ByteBuf buf) {
        players = readList(buf);
        resources = readList(buf);
        mobs = readList(buf);
        balances = readList(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        writeList(buf, players);
        writeList(buf, resources);
        writeList(buf, mobs);
        writeList(buf, balances);
    }

    private static void writeList(ByteBuf buf, List<String> values) {
        buf.writeInt(values.size());
        for (String value : values) ByteBufUtils.writeUTF8String(buf, value == null ? "" : value);
    }

    private static List<String> readList(ByteBuf buf) {
        int size = Math.max(0, Math.min(4096, buf.readInt()));
        List<String> result = new ArrayList<>(size);
        for (int i = 0; i < size; i++) result.add(ByteBufUtils.readUTF8String(buf));
        return result;
    }
}

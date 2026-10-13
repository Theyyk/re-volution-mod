package com.example.customguimod;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

import java.util.ArrayList;
import java.util.List;

public class DeckPacket implements IMessage {

    private List<String> deckNames = new ArrayList<>();
    private int activeDeck;

    public DeckPacket() {}

    public DeckPacket(List<String> deckNames, int activeDeck) {
        this.deckNames = new ArrayList<>();
        for (int i = 0; i < deckNames.size(); i++) {
            this.deckNames.add(RuneStatusRegistry.displayNameForDeck(i, deckNames.get(i)));
        }
        this.activeDeck = activeDeck;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        deckNames.clear();
        int count = buf.readInt();
        for (int i = 0; i < count; i++) {
            deckNames.add(ByteBufUtils.readUTF8String(buf));
        }
        activeDeck = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(deckNames.size());
        for (String deckName : deckNames) {
            ByteBufUtils.writeUTF8String(buf, deckName == null ? "" : deckName);
        }
        buf.writeInt(activeDeck);
    }

    public List<String> getDeckNames() {
        return deckNames;
    }

    public int getActiveDeck() {
        return activeDeck;
    }
}

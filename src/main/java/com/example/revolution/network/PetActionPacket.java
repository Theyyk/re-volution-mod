package com.example.revolution.network;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

/** Client intent only; all Pet validation remains server authoritative. */
public final class PetActionPacket implements IMessage {
    private String action = "";
    private String petId = "";

    public PetActionPacket() {}
    public PetActionPacket(String action) { this(action, ""); }
    public PetActionPacket(String action, String petId) {
        this.action = action == null ? "" : action;
        this.petId = petId == null ? "" : petId;
    }

    public String getAction() { return action; }
    public String getPetId() { return petId; }

    @Override public void fromBytes(ByteBuf buf) {
        action = ByteBufUtils.readUTF8String(buf);
        petId = ByteBufUtils.readUTF8String(buf);
    }
    @Override public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, action);
        ByteBufUtils.writeUTF8String(buf, petId);
    }
}

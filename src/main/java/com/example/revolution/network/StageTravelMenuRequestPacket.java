package com.example.revolution.network;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

/** Client request for the player's current stage-travel menu snapshot. */
public final class StageTravelMenuRequestPacket implements IMessage {
    @Override public void fromBytes(ByteBuf buf) {}
    @Override public void toBytes(ByteBuf buf) {}
}

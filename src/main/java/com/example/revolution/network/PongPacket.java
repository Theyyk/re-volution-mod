package com.example.revolution.network;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

public class PongPacket implements IMessage {

    private int balance;

    public PongPacket() {}

    public PongPacket(int balance) {
        this.balance = balance;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        balance = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(balance);
    }

    public int getBalance() {
        return balance;
    }
}

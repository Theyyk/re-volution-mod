package com.example.revolution.network;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class BalancePacket implements IMessage {

    private int balance;

    public BalancePacket() {}

    public BalancePacket(int balance) {
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

    public static class Handler implements IMessageHandler<BalancePacket, IMessage> {
        @Override
        @SideOnly(Side.CLIENT)
        public IMessage onMessage(BalancePacket message, MessageContext ctx) {
            // На сервере этот код не выполняется
            // На клиенте — обрабатывается в клиентском моде
            System.out.println("BalancePacket: " + message.balance);
            return null;
        }
    }
}

package com.example.revolution.network;

import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

/** Read-only server snapshot of the canonical Run Exchange; no client-owned economy state. */
public final class RunExchangeSnapshotPacket implements IMessage {
    public static final class OfferEntry {
        public String offerId = "";
        public String paymentCurrency = "";
        public String target = "";
        public String cost = "";
        public String reward = "";
        public String unavailableReason = "";
        public int purchaseCount;
        public boolean available;
        public OfferEntry() {}
        public OfferEntry(String offerId, String paymentCurrency, String target, String cost,
                          String reward, int purchaseCount, boolean available, String unavailableReason) {
            this.offerId = safe(offerId);
            this.paymentCurrency = safe(paymentCurrency);
            this.target = safe(target);
            this.cost = safe(cost);
            this.reward = safe(reward);
            this.purchaseCount = purchaseCount;
            this.available = available;
            this.unavailableReason = safe(unavailableReason);
        }
    }
    public long requestId;
    public String runId = "";
    public String fragments = "0";
    public String crystals = "0";
    public String response = "";
    public boolean unlocked;
    private final List<OfferEntry> offers = new ArrayList<>();

    public RunExchangeSnapshotPacket() {}
    public List<OfferEntry> getOffers() { return Collections.unmodifiableList(offers); }
    public void addOffer(OfferEntry entry) { if (entry != null && offers.size() < 16) offers.add(entry); }

    @Override public void fromBytes(ByteBuf buf) {
        requestId = buf.readLong();
        unlocked = buf.readBoolean();
        runId = read(buf);
        fragments = read(buf);
        crystals = read(buf);
        response = read(buf);
        offers.clear();
        int count = buf.readInt();
        if (count < 0 || count > 16) throw new IllegalArgumentException("Invalid Run Exchange offer count");
        for (int i = 0; i < count; i++) {
            OfferEntry e = new OfferEntry();
            e.offerId = read(buf);
            e.paymentCurrency = read(buf);
            e.target = read(buf);
            e.cost = read(buf);
            e.reward = read(buf);
            e.purchaseCount = buf.readInt();
            e.available = buf.readBoolean();
            e.unavailableReason = read(buf);
            offers.add(e);
        }
    }
    @Override public void toBytes(ByteBuf buf) {
        buf.writeLong(requestId);
        buf.writeBoolean(unlocked);
        write(buf, runId);
        write(buf, fragments);
        write(buf, crystals);
        write(buf, response);
        buf.writeInt(offers.size());
        for (OfferEntry e : offers) {
            write(buf, e.offerId);
            write(buf, e.paymentCurrency);
            write(buf, e.target);
            write(buf, e.cost);
            write(buf, e.reward);
            buf.writeInt(e.purchaseCount);
            buf.writeBoolean(e.available);
            write(buf, e.unavailableReason);
        }
    }
    private static String read(ByteBuf buf) { return ByteBufUtils.readUTF8String(buf); }
    private static void write(ByteBuf buf, String s) { ByteBufUtils.writeUTF8String(buf, safe(s)); }
    private static String safe(String s) { return s == null ? "" : s; }
}

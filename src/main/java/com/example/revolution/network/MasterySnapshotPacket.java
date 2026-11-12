package com.example.revolution.network;

import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

/** Read-only, server-authored Mastery projection; never persisted on the client. */
public final class MasterySnapshotPacket implements IMessage {
    public static final class BranchEntry {
        public String id = "";
        public String source = "";
        public String best = "0";
        public int tier;
        public int metaCap;
        public int metaGain;
        public BranchEntry() {}
        public BranchEntry(String id, String source, String best, int tier, int metaCap, int metaGain) {
            this.id = id; this.source = source; this.best = best;
            this.tier = tier; this.metaCap = metaCap; this.metaGain = metaGain;
        }
    }
    public static final class RankEntry {
        public String branch = "";
        public String id = "";
        public String threshold = "0";
        public int metaLevel;
        public int metaCap;
        public RankEntry() {}
        public RankEntry(String branch, String id, String threshold, int metaLevel, int metaCap) {
            this.branch = branch; this.id = id; this.threshold = threshold;
            this.metaLevel = metaLevel; this.metaCap = metaCap;
        }
    }
    public long requestId;
    public boolean artsUnlocked;
    public String unlockRequirement = "";
    public String response = "";
    public String previewKind = "";
    public String previewDetails = "";
    public String runId = "";
    public String activeBranch = "";
    public String progress = "0";
    public int level;
    public String fragments = "0";
    public String masteryPending = "0";
    public String petPending = "0";
    public String ranks = "";
    public boolean masteryResetAvailable;
    public boolean premiumPreserveAvailable;
    public int premiumCrystalCost;
    public long startedAt;
    private final List<BranchEntry> branches = new ArrayList<>();
    private final List<RankEntry> permanentRanks = new ArrayList<>();

    public MasterySnapshotPacket() {}
    public List<BranchEntry> getBranches() { return Collections.unmodifiableList(branches); }
    public void addBranch(BranchEntry branch) { if (branches.size() < 16) branches.add(branch); }

    public List<RankEntry> getPermanentRanks() { return Collections.unmodifiableList(permanentRanks); }
    public void addPermanentRank(RankEntry rank) { if (permanentRanks.size() < 48) permanentRanks.add(rank); }

    @Override public void fromBytes(ByteBuf buf) {
        requestId = buf.readLong(); artsUnlocked = buf.readBoolean();
        unlockRequirement = read(buf); response = read(buf);
        previewKind = read(buf); previewDetails = read(buf);
        runId = read(buf); activeBranch = read(buf); progress = read(buf); level = buf.readInt();
        fragments = read(buf); masteryPending = read(buf); petPending = read(buf); ranks = read(buf);
        masteryResetAvailable = buf.readBoolean(); premiumPreserveAvailable = buf.readBoolean();
        premiumCrystalCost = buf.readInt(); startedAt = buf.readLong();
        branches.clear();
        int count = buf.readInt();
        if (count < 0 || count > 16) throw new IllegalArgumentException("Invalid Mastery branch count");
        for (int i = 0; i < count; i++) {
            BranchEntry entry = new BranchEntry();
            entry.id = read(buf); entry.source = read(buf); entry.best = read(buf);
            entry.tier = buf.readInt(); entry.metaCap = buf.readInt(); entry.metaGain = buf.readInt();
            branches.add(entry);
        }
        permanentRanks.clear();
        int rankCount = buf.readInt();
        if (rankCount < 0 || rankCount > 48) throw new IllegalArgumentException("Invalid Mastery rank count");
        for (int i = 0; i < rankCount; i++) {
            RankEntry rank = new RankEntry();
            rank.branch = read(buf); rank.id = read(buf); rank.threshold = read(buf);
            rank.metaLevel = buf.readInt(); rank.metaCap = buf.readInt();
            permanentRanks.add(rank);
        }
    }
    @Override public void toBytes(ByteBuf buf) {
        buf.writeLong(requestId); buf.writeBoolean(artsUnlocked);
        write(buf, unlockRequirement); write(buf, response);
        write(buf, previewKind); write(buf, previewDetails);
        write(buf, runId); write(buf, activeBranch); write(buf, progress); buf.writeInt(level);
        write(buf, fragments); write(buf, masteryPending); write(buf, petPending); write(buf, ranks);
        buf.writeBoolean(masteryResetAvailable); buf.writeBoolean(premiumPreserveAvailable);
        buf.writeInt(premiumCrystalCost); buf.writeLong(startedAt);
        buf.writeInt(branches.size());
        for (BranchEntry entry : branches) {
            write(buf, entry.id); write(buf, entry.source); write(buf, entry.best);
            buf.writeInt(entry.tier); buf.writeInt(entry.metaCap); buf.writeInt(entry.metaGain);
        }
        buf.writeInt(permanentRanks.size());
        for (RankEntry rank : permanentRanks) {
            write(buf, rank.branch); write(buf, rank.id); write(buf, rank.threshold);
            buf.writeInt(rank.metaLevel); buf.writeInt(rank.metaCap);
        }
    }
    private static String read(ByteBuf buf) { return ByteBufUtils.readUTF8String(buf); }
    private static void write(ByteBuf buf, String value) { ByteBufUtils.writeUTF8String(buf, value == null ? "" : value); }
}

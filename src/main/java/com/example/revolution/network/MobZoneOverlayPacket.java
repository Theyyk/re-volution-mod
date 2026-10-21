package com.example.revolution.network;

import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

/** Server-authorized snapshot of configured mob-zone geometry. */
public final class MobZoneOverlayPacket implements IMessage {
    public static final class Zone {
        private final int internalId;
        private final double centerX;
        private final double centerZ;
        private final double radius;

        public Zone(int internalId, double centerX, double centerZ, double radius) {
            this.internalId = internalId;
            this.centerX = centerX;
            this.centerZ = centerZ;
            this.radius = radius;
        }

        public int getInternalId() { return internalId; }
        public double getCenterX() { return centerX; }
        public double getCenterZ() { return centerZ; }
        public double getRadius() { return radius; }
    }

    private final List<Zone> zones = new ArrayList<>();

    public MobZoneOverlayPacket() {}

    public MobZoneOverlayPacket(List<Zone> zones) {
        if (zones != null) this.zones.addAll(zones);
    }

    public List<Zone> getZones() {
        return Collections.unmodifiableList(zones);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        zones.clear();
        int count = Math.max(0, Math.min(buf.readInt(), 4096));
        for (int i = 0; i < count; i++) {
            zones.add(new Zone(buf.readInt(), buf.readDouble(), buf.readDouble(), buf.readDouble()));
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(zones.size());
        for (Zone zone : zones) {
            buf.writeInt(zone.internalId);
            buf.writeDouble(zone.centerX);
            buf.writeDouble(zone.centerZ);
            buf.writeDouble(zone.radius);
        }
    }
}

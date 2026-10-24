package com.example.revolution.network;

import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

/** Server-authorized snapshot of configured mob-zone and stage-trigger geometry. */
public final class MobZoneOverlayPacket implements IMessage {
    public static final class Zone {
        private final int internalId;
        private final double centerX;
        private final double centerZ;
        private final double radius;
        private final boolean hasTrigger;
        private final double triggerInnerRadius;
        private final double triggerOuterRadius;
        private final double forwardX;
        private final double forwardZ;

        public Zone(int internalId, double centerX, double centerZ, double radius) {
            this(internalId, centerX, centerZ, radius, false, 0.0D, 0.0D, 0.0D, 0.0D);
        }

        public Zone(
                int internalId,
                double centerX,
                double centerZ,
                double radius,
                boolean hasTrigger,
                double triggerInnerRadius,
                double triggerOuterRadius,
                double forwardX,
                double forwardZ) {
            this.internalId = internalId;
            this.centerX = centerX;
            this.centerZ = centerZ;
            this.radius = radius;
            this.hasTrigger = hasTrigger;
            this.triggerInnerRadius = triggerInnerRadius;
            this.triggerOuterRadius = triggerOuterRadius;
            this.forwardX = forwardX;
            this.forwardZ = forwardZ;
        }

        public int getInternalId() { return internalId; }
        public double getCenterX() { return centerX; }
        public double getCenterZ() { return centerZ; }
        public double getRadius() { return radius; }
        public boolean hasTrigger() { return hasTrigger; }
        public double getTriggerInnerRadius() { return triggerInnerRadius; }
        public double getTriggerOuterRadius() { return triggerOuterRadius; }
        public double getForwardX() { return forwardX; }
        public double getForwardZ() { return forwardZ; }
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
            int internalId = buf.readInt();
            double centerX = buf.readDouble();
            double centerZ = buf.readDouble();
            double radius = buf.readDouble();
            boolean hasTrigger = buf.readBoolean();
            double inner = 0.0D;
            double outer = 0.0D;
            double forwardX = 0.0D;
            double forwardZ = 0.0D;
            if (hasTrigger) {
                inner = buf.readDouble();
                outer = buf.readDouble();
                forwardX = buf.readDouble();
                forwardZ = buf.readDouble();
            }
            zones.add(new Zone(internalId, centerX, centerZ, radius,
                    hasTrigger, inner, outer, forwardX, forwardZ));
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
            buf.writeBoolean(zone.hasTrigger);
            if (zone.hasTrigger) {
                buf.writeDouble(zone.triggerInnerRadius);
                buf.writeDouble(zone.triggerOuterRadius);
                buf.writeDouble(zone.forwardX);
                buf.writeDouble(zone.forwardZ);
            }
        }
    }
}

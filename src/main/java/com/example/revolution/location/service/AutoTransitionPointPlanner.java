package com.example.revolution.location.service;

import com.example.revolution.location.definition.LocationDefinition;

/** Pure geometry for a generated stage destination placed toward its backward trigger half. */
public final class AutoTransitionPointPlanner {
    private static final double MIN_AXIS_LENGTH_SQUARED = 1.0E-12D;
    private static final double MAX_RADIUS_FRACTION = 0.9D;

    public static final class Point {
        private final double x;
        private final double y;
        private final double z;
        private final float yaw;
        private final float pitch;

        Point(double x, double y, double z, float yaw, float pitch) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.pitch = pitch;
        }

        public double getX() { return x; }
        public double getY() { return y; }
        public double getZ() { return z; }
        public float getYaw() { return yaw; }
        public float getPitch() { return pitch; }
    }

    /**
     * Uses the same next-first axis convention as stage triggers. The destination is halfway from the
     * stage center to the trigger inner boundary, clamped inside the mob zone for extreme offsets.
     */
    public Point plan(
            LocationDefinition source,
            LocationDefinition previous,
            LocationDefinition next,
            double triggerOffset,
            double y) {
        if (source == null) throw new IllegalArgumentException("source is required");
        if (!Double.isFinite(triggerOffset) || triggerOffset < 0.0D) {
            throw new IllegalArgumentException("triggerOffset must be non-negative and finite");
        }
        if (!Double.isFinite(y)) throw new IllegalArgumentException("y must be finite");

        double axisX;
        double axisZ;
        if (next != null) {
            axisX = next.getCenterX() - source.getCenterX();
            axisZ = next.getCenterZ() - source.getCenterZ();
            if (axisX * axisX + axisZ * axisZ <= MIN_AXIS_LENGTH_SQUARED) {
                axisX = 0.0D;
                axisZ = 0.0D;
            }
        } else {
            axisX = 0.0D;
            axisZ = 0.0D;
        }
        if (axisX * axisX + axisZ * axisZ <= MIN_AXIS_LENGTH_SQUARED && previous != null) {
            axisX = source.getCenterX() - previous.getCenterX();
            axisZ = source.getCenterZ() - previous.getCenterZ();
        }
        double lengthSquared = axisX * axisX + axisZ * axisZ;
        if (lengthSquared <= MIN_AXIS_LENGTH_SQUARED) return null;

        double length = Math.sqrt(lengthSquared);
        double forwardX = axisX / length;
        double forwardZ = axisZ / length;
        double innerTriggerRadius = source.getRadius() + triggerOffset;
        double distance = Math.min(innerTriggerRadius * 0.5D, source.getRadius() * MAX_RADIUS_FRACTION);

        // Backward is opposite the authoritative forward trigger axis.
        double x = source.getCenterX() - forwardX * distance;
        double z = source.getCenterZ() - forwardZ * distance;
        double faceCenterX = source.getCenterX() - x;
        double faceCenterZ = source.getCenterZ() - z;
        float yaw = (float) Math.toDegrees(Math.atan2(-faceCenterX, faceCenterZ));
        return new Point(x, y, z, yaw, 0.0F);
    }
}

package com.example.revolution.location.definition;

/** Circular gameplay Location shared by Realm geometry. */
public final class LocationDefinition {
    private final int locationId;
    private final String name;
    private final double centerX;
    private final double centerZ;
    private final double radius;
    private final boolean transitionPointConfigured;
    private final double transitionX;
    private final double transitionY;
    private final double transitionZ;
    private final float transitionYaw;
    private final float transitionPitch;

    public LocationDefinition(int locationId, String name, double centerX, double centerZ, double radius) {
        this(locationId, name, centerX, centerZ, radius, false, 0.0D, 0.0D, 0.0D, 0.0F, 0.0F);
    }

    public LocationDefinition(
            int locationId,
            String name,
            double centerX,
            double centerZ,
            double radius,
            double transitionX,
            double transitionY,
            double transitionZ,
            float transitionYaw,
            float transitionPitch) {
        this(locationId, name, centerX, centerZ, radius, true,
                transitionX, transitionY, transitionZ, transitionYaw, transitionPitch);
    }

    private LocationDefinition(
            int locationId,
            String name,
            double centerX,
            double centerZ,
            double radius,
            boolean transitionPointConfigured,
            double transitionX,
            double transitionY,
            double transitionZ,
            float transitionYaw,
            float transitionPitch) {
        if (locationId < 0) throw new IllegalArgumentException("locationId must be non-negative");
        if (name == null || name.trim().isEmpty()) throw new IllegalArgumentException("name is required");
        if (!Double.isFinite(centerX) || !Double.isFinite(centerZ)) throw new IllegalArgumentException("center coordinates must be finite");
        if (!Double.isFinite(radius) || radius <= 0.0D) throw new IllegalArgumentException("radius must be positive and finite");
        if (transitionPointConfigured) {
            if (!Double.isFinite(transitionX) || !Double.isFinite(transitionY) || !Double.isFinite(transitionZ)) {
                throw new IllegalArgumentException("transition coordinates must be finite");
            }
            if (!Float.isFinite(transitionYaw) || !Float.isFinite(transitionPitch)) {
                throw new IllegalArgumentException("transition rotation must be finite");
            }
        }
        this.locationId = locationId;
        this.name = name;
        this.centerX = centerX;
        this.centerZ = centerZ;
        this.radius = radius;
        this.transitionPointConfigured = transitionPointConfigured;
        this.transitionX = transitionX;
        this.transitionY = transitionY;
        this.transitionZ = transitionZ;
        this.transitionYaw = transitionYaw;
        this.transitionPitch = transitionPitch;
    }

    public int getLocationId() { return locationId; }
    public String getName() { return name; }
    public double getCenterX() { return centerX; }
    public double getCenterZ() { return centerZ; }
    public double getRadius() { return radius; }
    public boolean hasTransitionPoint() { return transitionPointConfigured; }
    public double getTransitionX() { return transitionX; }
    public double getTransitionY() { return transitionY; }
    public double getTransitionZ() { return transitionZ; }
    public float getTransitionYaw() { return transitionYaw; }
    public float getTransitionPitch() { return transitionPitch; }

    public boolean contains(double x, double z) {
        double dx = x - centerX;
        double dz = z - centerZ;
        return dx * dx + dz * dz <= radius * radius;
    }
}

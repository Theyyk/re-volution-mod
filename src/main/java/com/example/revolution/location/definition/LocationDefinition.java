package com.example.revolution.location.definition;

/** Circular gameplay Location shared by Realm geometry. */
public final class LocationDefinition {
    private final int locationId;
    private final String name;
    private final double centerX;
    private final double centerZ;
    private final double radius;

    public LocationDefinition(int locationId, String name, double centerX, double centerZ, double radius) {
        if (locationId < 0) throw new IllegalArgumentException("locationId must be non-negative");
        if (name == null || name.trim().isEmpty()) throw new IllegalArgumentException("name is required");
        if (!Double.isFinite(centerX) || !Double.isFinite(centerZ)) throw new IllegalArgumentException("center coordinates must be finite");
        if (!Double.isFinite(radius) || radius <= 0.0D) throw new IllegalArgumentException("radius must be positive and finite");
        this.locationId = locationId;
        this.name = name;
        this.centerX = centerX;
        this.centerZ = centerZ;
        this.radius = radius;
    }

    public int getLocationId() { return locationId; }
    public String getName() { return name; }
    public double getCenterX() { return centerX; }
    public double getCenterZ() { return centerZ; }
    public double getRadius() { return radius; }

    public boolean contains(double x, double z) {
        double dx = x - centerX;
        double dz = z - centerZ;
        return dx * dx + dz * dz <= radius * radius;
    }
}

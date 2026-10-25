package com.example.revolution.location.generation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Immutable template contract. It describes geometry and allowed decoration families, not Minecraft blocks. */
public final class LocationTemplate {
    private final String templateId;
    private final double radius;
    private final List<LocationDecorationKind> decorationKinds;

    public LocationTemplate(String templateId, double radius, List<LocationDecorationKind> decorationKinds) {
        if (templateId == null || templateId.trim().isEmpty()) throw new IllegalArgumentException("templateId is required");
        if (!Double.isFinite(radius) || radius <= 0.0D) throw new IllegalArgumentException("radius must be positive and finite");
        if (decorationKinds == null) throw new IllegalArgumentException("decorationKinds is required");
        List<LocationDecorationKind> copy = new ArrayList<>();
        for (LocationDecorationKind kind : decorationKinds) {
            if (kind == null) throw new IllegalArgumentException("decoration kind is required");
            if (!copy.contains(kind)) copy.add(kind);
        }
        this.templateId = templateId;
        this.radius = radius;
        this.decorationKinds = Collections.unmodifiableList(copy);
    }

    /** Initial v1.3.12 template. Real construction/template assets may replace this later. */
    public static LocationTemplate defaultStage() {
        return new LocationTemplate("default-stage-v1", 30.0D, Arrays.asList(
                LocationDecorationKind.FLOWER_CLEARING,
                LocationDecorationKind.TREE_CLUSTER,
                LocationDecorationKind.ROCK_CLUSTER));
    }

    public String getTemplateId() { return templateId; }
    public double getRadius() { return radius; }
    public List<LocationDecorationKind> getDecorationKinds() { return decorationKinds; }
}

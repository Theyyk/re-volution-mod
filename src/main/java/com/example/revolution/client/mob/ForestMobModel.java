package com.example.revolution.client.mob;

import com.example.revolution.mob.appearance.MobAnimationState;
import com.example.revolution.mob.appearance.MobAppearanceDefinition;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Box-model family rigs for all eight forest families plus the unique Location 1 boss. */
@SideOnly(Side.CLIENT)
public final class ForestMobModel extends ModelBase {
    private final List<ModelRenderer> parts = new ArrayList<>();
    private final List<ModelRenderer> legs = new ArrayList<>();
    private final List<ModelRenderer> arms = new ArrayList<>();
    private ModelRenderer head;
    private ModelRenderer tail;
    private MobAnimationState animation = MobAnimationState.IDLE;
    private float animationAge;

    public ForestMobModel(MobAppearanceDefinition appearance) {
        textureWidth = 64;
        textureHeight = 64;
        String family = appearance.getFamilyId();
        if ("turtle".equals(family)) buildTurtle(appearance.getAppearanceId());
        else if ("serpent".equals(family)) buildSerpent(appearance.getAppearanceId());
        else if ("mantis".equals(family)) buildMantis(appearance.getAppearanceId());
        else if ("spore".equals(family)) buildSpore(appearance.getAppearanceId());
        else if ("golem".equals(family)) buildGolem(appearance.getAppearanceId());
        else if ("spirit".equals(family)) buildSpirit(appearance.getAppearanceId());
        else if ("beast".equals(family)) buildBeast(appearance.getAppearanceId());
        else if ("dryad".equals(family)) buildDryad(appearance.getAppearanceId());
        else {
            textureWidth = 128;
            textureHeight = 128;
            buildBoss(appearance.getAppearanceId());
        }
    }

    public void animate(MobAnimationState state, EntityLivingBase entity, float partialTicks) {
        this.animation = state == null ? MobAnimationState.IDLE : state;
        this.animationAge = entity.ticksExisted + partialTicks;
    }

    @Override
    public void render(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks,
                       float netHeadYaw, float headPitch, float scale) {
        resetAnimatedParts();
        applyAnimation(limbSwing, limbSwingAmount, netHeadYaw, headPitch);
        for (ModelRenderer part : parts) part.render(scale);
    }

    private void resetAnimatedParts() {
        if (head != null) { head.rotateAngleX = 0.0F; head.rotateAngleY = 0.0F; head.rotateAngleZ = 0.0F; }
        if (tail != null) { tail.rotateAngleX = 0.0F; tail.rotateAngleY = 0.0F; tail.rotateAngleZ = 0.0F; }
        for (ModelRenderer leg : legs) { leg.rotateAngleX = 0.0F; leg.rotateAngleY = 0.0F; leg.rotateAngleZ = 0.0F; }
        for (ModelRenderer arm : arms) { arm.rotateAngleX = 0.0F; arm.rotateAngleY = 0.0F; arm.rotateAngleZ = 0.0F; }
    }

    private void applyAnimation(float limbSwing, float limbSwingAmount, float netHeadYaw, float headPitch) {
        float idle = MathHelper.sin(animationAge * 0.10F) * 0.045F;
        if (head != null) {
            head.rotateAngleY = netHeadYaw * 0.017453292F;
            head.rotateAngleX = headPitch * 0.017453292F + idle;
        }
        if (tail != null) tail.rotateAngleY = MathHelper.sin(animationAge * 0.14F) * 0.18F;

        if (animation == MobAnimationState.WALK) {
            float swing = MathHelper.cos(limbSwing * 0.6662F) * 1.1F * Math.max(0.25F, limbSwingAmount);
            for (int i = 0; i < legs.size(); i++) legs.get(i).rotateAngleX = ((i & 1) == 0 ? swing : -swing);
            for (int i = 0; i < arms.size(); i++) arms.get(i).rotateAngleX = ((i & 1) == 0 ? -swing * 0.65F : swing * 0.65F);
        } else {
            for (int i = 0; i < arms.size(); i++) arms.get(i).rotateAngleZ = ((i & 1) == 0 ? idle : -idle);
        }

        if (animation == MobAnimationState.HURT) {
            float recoil = MathHelper.sin(animationAge * 2.4F) * 0.18F;
            if (head != null) head.rotateAngleX -= 0.18F;
            for (ModelRenderer arm : arms) arm.rotateAngleZ += recoil;
        }
        if (animation == MobAnimationState.DEATH) {
            if (head != null) head.rotateAngleX = 0.55F;
            for (ModelRenderer arm : arms) arm.rotateAngleX = -0.55F;
        }
    }

    private ModelRenderer box(float px, float py, float pz, int x, int y, int z, int w, int h, int d) {
        return box(px, py, pz, x, y, z, w, h, d, 0, 0);
    }

    private ModelRenderer box(float px, float py, float pz, int x, int y, int z,
                              int w, int h, int d, int textureU, int textureV) {
        ModelRenderer part = new ModelRenderer(this, textureU, textureV);
        part.setRotationPoint(px, py, pz);
        part.addBox(x, y, z, w, h, d);
        parts.add(part);
        return part;
    }

    private void buildTurtle(String id) {
        // shell 0,0 | body 0,20 | legs 0,38 | tail 20,38 | head 42,37 | extras y=48
        box(0, 19, 0, -6, -4, -7, 12, 5, 14, 0, 0);
        box(0, 20, 0, -5, -2, -6, 10, 4, 12, 0, 20);
        head = box(0, 20, -7, -3, -2, -4, 6, 4, 5, 42, 37);
        legs.add(box(-5, 22, -4, -2, -1, -2, 4, 3, 5, 0, 38));
        legs.add(box(5, 22, -4, -2, -1, -2, 4, 3, 5, 0, 38));
        legs.add(box(-5, 22, 4, -2, -1, -2, 4, 3, 5, 0, 38));
        legs.add(box(5, 22, 4, -2, -1, -2, 4, 3, 5, 0, 38));
        tail = box(0, 21, 7, -1, -1, 0, 2, 2, 4, 20, 38);
        if (id.contains("thorn") || id.contains("ancient")) {
            box(-4, 15, -2, -1, -4, -1, 2, 4, 2, 0, 48);
            box(4, 15, 2, -1, -4, -1, 2, 4, 2, 0, 48);
        }
        if (id.contains("root") || id.contains("ancient")) {
            box(0, 15, 3, -1, -5, -1, 2, 5, 2, 10, 48);
        }
        if (id.contains("bloom")) {
            box(-3, 15, 2, -2, -2, -2, 4, 2, 4, 20, 48);
            box(3, 15, -2, -2, -2, -2, 4, 2, 4, 20, 48);
        }
    }

    private void buildSerpent(String id) {
        // head 0,0 | body A 32,0 | body B 0,14 | tail 26,14 | plate 0,28 | spikes 30,28
        for (int i = 0; i < 5; i++) {
            int u = (i & 1) == 0 ? 32 : 0;
            int v = (i & 1) == 0 ? 0 : 14;
            box(0, 19 + i, i * 3 - 5, -3, -3, -3, 6, 6, 6, u, v);
        }
        head = box(0, 18, -9, -4, -3, -5, 8, 6, 7, 0, 0);
        tail = box(0, 23, 10, -2, -2, -2, 4, 4, 8, 26, 14);
        if (id.contains("thorn") || id.contains("ancient")) {
            for (int i = 0; i < 4; i++) box(0, 15 + i, i * 3 - 4, -1, -4, -1, 2, 4, 2, 30, 28);
        }
        if (id.contains("root") || id.contains("wood") || id.contains("ancient")) {
            box(0, 17, -5, -5, -2, -2, 10, 3, 4, 0, 28);
        }
    }

    private void buildMantis(String id) {
        // head 0,0 | torso 30,0 | forearms 0,16 | rear legs 14,16 | support 28,16 | thorn 38,16 | emerald 48,16
        box(0, 17, 0, -3, -5, -2, 6, 10, 4, 30, 0);
        head = box(0, 12, -1, -4, -4, -3, 8, 5, 6, 0, 0);
        legs.add(box(-4, 18, 0, -2, 0, -2, 3, 9, 3, 14, 16));
        legs.add(box(4, 18, 0, -1, 0, -2, 3, 9, 3, 14, 16));
        arms.add(box(-4, 14, -1, -2, -1, -2, 3, 10, 3, 0, 16));
        arms.add(box(4, 14, -1, -1, -1, -2, 3, 10, 3, 0, 16));
        box(-6, 20, 2, -1, 0, -1, 2, 7, 2, 28, 16);
        box(6, 20, 2, -1, 0, -1, 2, 7, 2, 28, 16);
        if (id.contains("thorn") || id.contains("ancient")) {
            box(-3, 13, 1, -1, -4, -1, 2, 4, 2, 38, 16);
            box(3, 13, 1, -1, -4, -1, 2, 4, 2, 38, 16);
        }
        if (id.contains("emerald")) box(0, 17, -3, -2, -2, -2, 4, 5, 2, 48, 16);
    }

    private void buildSpore(String id) {
        // cap 0,0 | body 0,20 | legs 34,20 | crawler legs 48,20 | growths 34,28
        box(0, 18, 0, -4, -5, -4, 8, 9, 8, 0, 20);
        head = box(0, 13, 0, -7, -4, -7, 14, 5, 14, 0, 0);
        legs.add(box(-3, 22, -2, -2, 0, -2, 3, 5, 3, 34, 20));
        legs.add(box(3, 22, -2, -1, 0, -2, 3, 5, 3, 34, 20));
        if (id.contains("crawler") || id.contains("guardian") || id.contains("mycelium")) {
            legs.add(box(-5, 21, 2, -2, 0, -1, 4, 3, 2, 48, 20));
            legs.add(box(5, 21, 2, -2, 0, -1, 4, 3, 2, 48, 20));
        }
        if (id.contains("guardian") || id.contains("mycelium")) {
            box(-4, 10, 0, -2, -4, -2, 4, 5, 4, 34, 28);
            box(4, 11, 2, -2, -4, -2, 4, 5, 4, 34, 28);
        }
    }

    private void buildGolem(String id) {
        // torso 0,0 | arms 40,0 | head 0,22 | legs 40,22 | moss 0,40 | roots 50,40
        box(0, 15, 0, -6, -7, -4, 12, 13, 8, 0, 0);
        head = box(0, 7, 0, -5, -5, -5, 10, 7, 10, 0, 22);
        arms.add(box(-8, 13, 0, -4, -2, -4, 5, 15, 7, 40, 0));
        arms.add(box(8, 13, 0, -1, -2, -4, 5, 15, 7, 40, 0));
        legs.add(box(-3, 21, 0, -3, 0, -3, 5, 8, 6, 40, 22));
        legs.add(box(3, 21, 0, -2, 0, -3, 5, 8, 6, 40, 22));
        if (id.contains("root") || id.contains("ancient")) {
            box(-5, 8, 2, -1, -6, -1, 2, 7, 2, 50, 40);
            box(5, 8, 2, -1, -6, -1, 2, 7, 2, 50, 40);
        }
        if (id.contains("moss")) box(0, 7, 0, -6, -2, -6, 12, 2, 12, 0, 40);
    }

    private void buildSpirit(String id) {
        // crown 0,0 | head 0,16 | torso 34,16 | tail 0,32 | arms 22,34 | flowers 36,34
        box(0, 15, 0, -4, -6, -3, 8, 11, 6, 34, 16);
        head = box(0, 8, 0, -4, -4, -4, 8, 7, 8, 0, 16);
        arms.add(box(-5, 15, 0, -2, -2, -2, 3, 9, 3, 22, 34));
        arms.add(box(5, 15, 0, -1, -2, -2, 3, 9, 3, 22, 34));
        tail = box(0, 21, 1, -3, 0, -2, 6, 7, 4, 0, 32);
        if (id.contains("flower")) {
            box(-4, 6, 0, -2, -2, -2, 4, 3, 4, 36, 34);
            box(4, 7, 0, -2, -2, -2, 4, 3, 4, 36, 34);
        }
        if (id.contains("keeper") || id.contains("ancient")) box(0, 5, 0, -6, -2, -6, 12, 2, 12, 0, 0);
    }

    private void buildBeast(String id) {
        // torso 0,0 | head 0,26 | legs 34,26 | tail 0,42 | antlers 24,42 | thorns 34,42 | ancient antlers 44,42
        box(0, 16, 1, -5, -5, -8, 10, 9, 16, 0, 0);
        head = box(0, 12, -8, -4, -4, -5, 8, 7, 8, 0, 26);
        legs.add(box(-4, 20, -4, -2, 0, -2, 3, 8, 3, 34, 26));
        legs.add(box(4, 20, -4, -1, 0, -2, 3, 8, 3, 34, 26));
        legs.add(box(-4, 20, 5, -2, 0, -2, 3, 8, 3, 34, 26));
        legs.add(box(4, 20, 5, -1, 0, -2, 3, 8, 3, 34, 26));
        tail = box(0, 16, 8, -2, -2, 0, 4, 4, 7, 0, 42);
        box(-3, 8, -8, -1, -7, -1, 2, 8, 2, 24, 42);
        box(3, 8, -8, -1, -7, -1, 2, 8, 2, 24, 42);
        if (id.contains("thorn") || id.contains("keeper")) {
            box(-5, 12, -2, -1, -5, -1, 2, 6, 2, 34, 42);
            box(5, 12, -2, -1, -5, -1, 2, 6, 2, 34, 42);
        }
        if (id.contains("ancient") || id.contains("keeper")) {
            box(-5, 3, -8, -4, -1, -1, 5, 2, 2, 44, 42);
            box(5, 3, -8, -1, -1, -1, 5, 2, 2, 44, 42);
        }
    }

    private void buildDryad(String id) {
        // head 0,0 | torso 34,0 | arms 0,16 | legs 14,16 | staff 28,16 | guardian 38,16 | thorn 0,32 | flowers 10,32
        box(0, 15, 0, -4, -6, -2, 8, 11, 4, 34, 0);
        head = box(0, 7, 0, -4, -4, -4, 8, 7, 8, 0, 0);
        arms.add(box(-5, 14, 0, -2, -2, -2, 3, 12, 3, 0, 16));
        arms.add(box(5, 14, 0, -1, -2, -2, 3, 12, 3, 0, 16));
        legs.add(box(-2, 20, 0, -2, 0, -2, 3, 8, 3, 14, 16));
        legs.add(box(2, 20, 0, -1, 0, -2, 3, 8, 3, 14, 16));
        box(7, 13, 0, -1, -5, -1, 2, 19, 2, 28, 16);
        if (id.contains("thorn") || id.contains("ancient")) {
            box(-4, 4, 0, -1, -5, -1, 2, 6, 2, 0, 32);
            box(4, 4, 0, -1, -5, -1, 2, 6, 2, 0, 32);
        }
        if (id.contains("flower")) {
            box(-4, 4, 0, -2, -2, -2, 4, 3, 4, 10, 32);
            box(4, 5, 0, -2, -2, -2, 4, 3, 4, 10, 32);
        }
        if (id.contains("guardian")) box(0, 14, 2, -5, -6, 0, 10, 12, 2, 38, 16);
    }

    private void buildBoss(String id) {
        // Dedicated 128x128 atlas for the unique Location 1 boss.
        box(0, 13, 0, -8, -9, -6, 16, 17, 12, 0, 0);
        head = box(0, 1, -1, -7, -6, -6, 14, 10, 12, 58, 0);
        arms.add(box(-11, 12, 0, -5, -3, -5, 7, 20, 9, 0, 32));
        arms.add(box(11, 12, 0, -2, -3, -5, 7, 20, 9, 0, 32));
        legs.add(box(-5, 21, 0, -4, 0, -4, 7, 10, 8, 34, 32));
        legs.add(box(5, 21, 0, -3, 0, -4, 7, 10, 8, 34, 32));
        box(-5, -3, -1, -1, -9, -1, 3, 11, 3, 66, 32);
        box(5, -3, -1, -2, -9, -1, 3, 11, 3, 66, 32);
        box(-9, -7, -1, -6, -1, -1, 7, 3, 3, 80, 32);
        box(9, -7, -1, -1, -1, -1, 7, 3, 3, 80, 32);
        box(0, 12, -7, -3, -3, -1, 6, 6, 2, 102, 32);
        tail = box(0, 12, 6, -3, -3, 0, 6, 6, 10, 0, 64);
    }
}

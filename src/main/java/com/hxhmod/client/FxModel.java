package com.hxhmod.client;

import com.hxhmod.HxhMod;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;

/** Alle 3D-Modelle der Effekte in einem Layer: Drachenkopf (mit beweglichem Kiefer), Körpersegment, Faust, Hand, Klinge, Stab. */
public final class FxModel {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(new ResourceLocation(HxhMod.MODID, "fx"), "main");
    public static final ResourceLocation TEXTURE = new ResourceLocation(HxhMod.MODID, "textures/entity/fx.png");

    public static LayerDefinition create() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // ---------- Drachenkopf (zeigt nach -Z)
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4f, -3f, -9f, 8f, 6f, 9f)
                .texOffs(0, 16).addBox(-3f, -1.5f, -15f, 6f, 3f, 6f)
                .texOffs(30, 0).addBox(-2.6f, -2.8f, -15.6f, 1.4f, 1.4f, 1.4f)
                .texOffs(30, 0).addBox(1.2f, -2.8f, -15.6f, 1.4f, 1.4f, 1.4f)
                .texOffs(0, 30).addBox(-4.4f, -2.4f, -7f, 0.8f, 1.6f, 3f)
                .texOffs(0, 30).addBox(3.6f, -2.4f, -7f, 0.8f, 1.6f, 3f)
                .texOffs(20, 24).addBox(-1f, -5f, -3f, 2f, 3f, 2f)
                .texOffs(20, 24).addBox(-1f, -6.5f, -0.5f, 2f, 4f, 2f)
                .texOffs(20, 24).addBox(-1f, -5f, 2f, 2f, 3f, 2f), PartPose.ZERO);
        head.addOrReplaceChild("jaw", CubeListBuilder.create()
                .texOffs(0, 40).addBox(-3f, 0f, -13f, 6f, 2f, 13f)
                .texOffs(30, 40).addBox(-2.6f, -1.2f, -12f, 0.8f, 1.2f, 0.8f)
                .texOffs(30, 40).addBox(1.8f, -1.2f, -12f, 0.8f, 1.2f, 0.8f)
                .texOffs(30, 40).addBox(-2.6f, -1.2f, -8f, 0.8f, 1.2f, 0.8f)
                .texOffs(30, 40).addBox(1.8f, -1.2f, -8f, 0.8f, 1.2f, 0.8f), PartPose.offset(0f, 3f, 0f));
        head.addOrReplaceChild("horn_l", CubeListBuilder.create().texOffs(20, 0).addBox(-1f, -7f, -1f, 2f, 7f, 2f),
                PartPose.offsetAndRotation(-3f, -3f, -1f, 0.9f, 0f, -0.35f));
        head.addOrReplaceChild("horn_r", CubeListBuilder.create().texOffs(20, 0).addBox(-1f, -7f, -1f, 2f, 7f, 2f),
                PartPose.offsetAndRotation(3f, -3f, -1f, 0.9f, 0f, 0.35f));
        head.addOrReplaceChild("whisker_l", CubeListBuilder.create().texOffs(0, 56).addBox(-0.3f, -0.3f, -22f, 0.6f, 0.6f, 22f),
                PartPose.offsetAndRotation(-2f, 0f, -12f, 0.1f, 0.55f, 0f));
        head.addOrReplaceChild("whisker_r", CubeListBuilder.create().texOffs(0, 56).addBox(-0.3f, -0.3f, -22f, 0.6f, 0.6f, 22f),
                PartPose.offsetAndRotation(2f, 0f, -12f, 0.1f, -0.55f, 0f));

        // ---------- Körpersegment
        root.addOrReplaceChild("seg", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-3f, -3f, -3f, 6f, 6f, 6f)
                .texOffs(24, 0).addBox(-0.8f, -6.5f, -1.5f, 1.6f, 4f, 3f)
                .texOffs(0, 16).addBox(-5.5f, -0.6f, -1.5f, 3f, 1.2f, 3f)
                .texOffs(0, 16).addBox(2.5f, -0.6f, -1.5f, 3f, 1.2f, 3f), PartPose.ZERO);

        // ---------- Faust (Knöchel zeigen nach -Z)
        root.addOrReplaceChild("fist", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-5f, -4f, -5f, 10f, 8f, 9f)
                .texOffs(0, 20).addBox(-5f, -5f, -7f, 2.4f, 2.2f, 4f)
                .texOffs(0, 20).addBox(-2.5f, -5f, -7f, 2.4f, 2.2f, 4f)
                .texOffs(0, 20).addBox(0f, -5f, -7f, 2.4f, 2.2f, 4f)
                .texOffs(0, 20).addBox(2.5f, -5f, -7f, 2.4f, 2.2f, 4f)
                .texOffs(20, 20).addBox(-5f, -4f, -8.5f, 2.4f, 6.5f, 2.6f)
                .texOffs(20, 20).addBox(-2.5f, -4f, -8.5f, 2.4f, 6.5f, 2.6f)
                .texOffs(20, 20).addBox(0f, -4f, -8.5f, 2.4f, 6.5f, 2.6f)
                .texOffs(20, 20).addBox(2.5f, -4f, -8.5f, 2.4f, 6.5f, 2.6f)
                .texOffs(34, 20).addBox(-8f, -2f, -6f, 3.5f, 3.5f, 7f), PartPose.ZERO);

        // ---------- Offene Hand mit 5 beweglichen Fingern
        PartDefinition palm = root.addOrReplaceChild("palm", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-5f, -1.2f, -4f, 10f, 2.4f, 8f), PartPose.ZERO);
        for (int i = 0; i < 5; i++) {
            palm.addOrReplaceChild("finger" + i, CubeListBuilder.create()
                    .texOffs(0, 16).addBox(-0.9f, -0.9f, -9f, 1.8f, 1.8f, 9f), PartPose.offset(-4f + i * 2f, 0f, -4f));
        }

        // ---------- Klinge (verjüngt sich zur Spitze), Länge 30 px
        root.addOrReplaceChild("blade", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-1.2f, -0.6f, -12f, 2.4f, 1.2f, 12f)
                .texOffs(0, 16).addBox(-0.8f, -0.5f, -22f, 1.6f, 1f, 10f)
                .texOffs(0, 28).addBox(-0.4f, -0.4f, -30f, 0.8f, 0.8f, 8f), PartPose.ZERO);

        // ---------- Stab (1 Block lang, wird für Blitze und Ringe gestreckt)
        root.addOrReplaceChild("bar", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-0.5f, -0.5f, -8f, 1f, 1f, 16f), PartPose.ZERO);

        // ---------- Schmetterlingsflügel (links/rechts), Drehpunkt am Rücken, Klappen um die Z-Achse
        float[][] upper = {{-12, 3, 8}, {-9, 4, 16}, {-5, 4, 22}, {-1, 4, 24}, {3, 4, 20}, {7, 3, 12}};
        float[][] lower = {{6, 3, 14}, {9, 4, 16}, {13, 4, 13}, {17, 3, 8}, {20, 2, 4}};
        for (int side = 0; side < 2; side++) {
            float sg = side == 0 ? 1f : -1f;
            CubeListBuilder wb = CubeListBuilder.create().texOffs(0, 0);
            for (float[][] set : new float[][][]{upper, lower}) {
                for (float[] r : set) {
                    float xs = sg > 0 ? 2f : -2f - r[2];
                    wb.addBox(xs, -0.5f, r[0], r[2], 1f, r[1]);
                }
            }
            for (float[] v : new float[][]{{-4f, 22f}, {1f, 24f}, {10f, 14f}}) {
                float xs = sg > 0 ? 2f : -2f - v[1];
                wb.addBox(xs, -1f, v[0], v[1], 2f, 0.8f);
            }
            for (float[] sp : new float[][]{{10f, -3f}, {8f, 12f}}) {
                float xs = sg > 0 ? sp[0] : -sp[0] - 4f;
                wb.addBox(xs, -1.3f, sp[1], 4f, 2.6f, 4f);
            }
            root.addOrReplaceChild(sg > 0 ? "wing_l" : "wing_r", wb, PartPose.ZERO);
        }

        return LayerDefinition.create(mesh, 64, 64);
    }

    private FxModel() {}
}

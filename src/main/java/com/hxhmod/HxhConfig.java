package com.hxhmod;

import net.minecraftforge.common.ForgeConfigSpec;

/** Alle Balancing-Werte an einem Ort (config/hxhmod-common.toml). */
public final class HxhConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.IntValue BASE_AURA, AURA_PER_LEVEL, BLOCK_LIMIT, DEBRIS_MAX;
    public static final ForgeConfigSpec.DoubleValue AURA_REGEN, TEN_REGEN_MULT, ZETSU_REGEN_MULT, REN_DRAIN;
    public static final ForgeConfigSpec.DoubleValue TEN_REDUCTION, TEN_AURA_PER_DAMAGE, ZETSU_TAKEN;
    public static final ForgeConfigSpec.DoubleValue DAMAGE_MULT, PVP_MULT, COST_MULT, COOLDOWN_MULT, MAX_HARDNESS;
    public static final ForgeConfigSpec.BooleanValue BLOCK_DAMAGE, DROP_BLOCKS, CUTSCENES, MODELS_3D;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        b.push("aura");
        BASE_AURA = b.comment("Maximale Aura auf Level 1").defineInRange("baseAura", 100, 10, 2000);
        AURA_PER_LEVEL = b.comment("Zusätzliche Max-Aura pro Level").defineInRange("auraPerLevel", 15, 0, 500);
        AURA_REGEN = b.comment("Aura-Regeneration pro Sekunde (offen)").defineInRange("regenPerSecond", 4.0, 0.0, 100.0);
        TEN_REGEN_MULT = b.comment("Regen-Multiplikator in Ten").defineInRange("tenRegenMult", 1.5, 0.0, 10.0);
        ZETSU_REGEN_MULT = b.comment("Regen-Multiplikator in Zetsu").defineInRange("zetsuRegenMult", 3.0, 0.0, 10.0);
        REN_DRAIN = b.comment("Aura-Verbrauch pro Sekunde in Ren").defineInRange("renDrainPerSecond", 3.0, 0.0, 100.0);
        b.pop();
        b.push("stances");
        TEN_REDUCTION = b.comment("Schadensreduktion in Ten (0.30 = 30%)").defineInRange("tenReduction", 0.30, 0.0, 0.9);
        TEN_AURA_PER_DAMAGE = b.comment("Aura-Kosten pro absorbiertem Schaden").defineInRange("tenAuraPerDamage", 1.5, 0.0, 20.0);
        ZETSU_TAKEN = b.comment("Schadensmultiplikator in Zetsu (1.25 = +25% Schaden)").defineInRange("zetsuDamageTaken", 1.25, 1.0, 3.0);
        b.pop();
        b.push("balance");
        DAMAGE_MULT = b.comment("Globaler Schadensmultiplikator").defineInRange("damageMultiplier", 1.0, 0.1, 10.0);
        PVP_MULT = b.comment("Schaden/Dauer-Multiplikator gegen Spieler").defineInRange("pvpMultiplier", 0.6, 0.1, 5.0);
        COST_MULT = b.comment("Globaler Aura-Kosten-Multiplikator").defineInRange("costMultiplier", 1.0, 0.1, 10.0);
        COOLDOWN_MULT = b.comment("Globaler Cooldown-Multiplikator").defineInRange("cooldownMultiplier", 1.0, 0.1, 10.0);
        b.pop();
        b.push("visuals");
        MODELS_3D = b.comment("3D-Modelle bei Attacken (Drache, Faust, Klingen, Blitze). Bei Performance-Problemen abschalten.").define("models3d", true);
        CUTSCENES = b.comment("Kamera-Cutscenes bei großen Attacken (Esc bricht ab)").define("cutscenes", true);
        b.pop();
        b.push("destruction");
        BLOCK_DAMAGE = b.comment("Fähigkeiten zerstören Blöcke (Krater, Schneisen)").define("blockDamage", true);
        DROP_BLOCKS = b.comment("Zerstörte Blöcke droppen Items (kann Lag erzeugen)").define("dropBlocks", false);
        MAX_HARDNESS = b.comment("Maximale Block-Härte, die überhaupt zerstört werden kann (Obsidian = 50)").defineInRange("maxHardness", 6.0, 0.0, 100.0);
        BLOCK_LIMIT = b.comment("Maximal zerstörte Blöcke pro Fähigkeit").defineInRange("blockLimit", 350, 10, 3000);
        DEBRIS_MAX = b.comment("Maximal herumfliegende Trümmer pro Fähigkeit").defineInRange("maxDebris", 40, 0, 200);
        b.pop();
        SPEC = b.build();
    }

    private HxhConfig() {}
}

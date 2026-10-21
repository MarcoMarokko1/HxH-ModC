package com.hxhmod.nen;

import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class Cooldowns {
    private static final Map<UUID, Map<String, Long>> MAP = new HashMap<>();

    public static int remaining(ServerPlayer p, String id) {
        Map<String, Long> m = MAP.get(p.getUUID());
        if (m == null) return 0;
        Long end = m.get(id);
        return end == null ? 0 : (int) Math.max(0, end - Scheduler.now());
    }

    public static void set(ServerPlayer p, String id, int ticks) {
        MAP.computeIfAbsent(p.getUUID(), k -> new HashMap<>()).put(id, Scheduler.now() + ticks);
    }

    public static void clear(UUID id) { MAP.remove(id); }

    private Cooldowns() {}
}

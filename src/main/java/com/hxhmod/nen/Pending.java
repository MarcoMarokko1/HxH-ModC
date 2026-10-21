package com.hxhmod.nen;

import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Laufender Zustand für "Riesen-Aufprall": SLAM = Startzeit, NO_FALL = Ende des Fallschaden-Schutzes. */
public final class Pending {
    public static final Map<UUID, Long> SLAM = new HashMap<>();
    public static final Map<UUID, Long> NO_FALL = new HashMap<>();

    public static boolean noFall(ServerPlayer p) {
        Long end = NO_FALL.get(p.getUUID());
        return end != null && Scheduler.now() < end;
    }

    public static void clear(UUID id) {
        SLAM.remove(id);
        NO_FALL.remove(id);
    }

    private Pending() {}
}

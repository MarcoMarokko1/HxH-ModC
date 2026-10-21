package com.hxhmod.client;

/** Clientseitiger Spiegel des Nen-Zustands (nur Anzeige, keine Logik). */
public final class ClientNenData {
    public static int type = -1, level = 1, xp = 0, stance = 0, selected = 0, cdLeft = 0, cdTotal = 1;
    public static float aura = 0, maxAura = 100;
    public static float shakeIntensity = 0;
    public static int shakeTicks = 0;
    public static int flashTicks = 0, flashMax = 1;
    public static float fovKick = 0;
    public static int fovTicks = 0;

    private ClientNenData() {}
}

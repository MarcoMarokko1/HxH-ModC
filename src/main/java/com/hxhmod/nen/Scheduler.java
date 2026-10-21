package com.hxhmod.nen;

import com.hxhmod.HxhMod;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Minimaler Tick-Scheduler für zeitversetzte Effekte (Salven, Aufladen, Zonen). */
public final class Scheduler {
    private record Task(long at, Runnable run) {}

    private static long now = 0;
    private static final List<Task> TASKS = new ArrayList<>();

    public static long now() { return now; }

    public static void later(int delayTicks, Runnable r) {
        TASKS.add(new Task(now + Math.max(1, delayTicks), r));
    }

    public static void tick() {
        now++;
        if (TASKS.isEmpty()) return;
        List<Task> due = new ArrayList<>();
        Iterator<Task> it = TASKS.iterator();
        while (it.hasNext()) {
            Task t = it.next();
            if (t.at <= now) {
                due.add(t);
                it.remove();
            }
        }
        for (Task t : due) {
            try {
                t.run.run();
            } catch (Exception ex) {
                HxhMod.LOGGER.error("Fehler in geplantem Nen-Effekt", ex);
            }
        }
    }

    public static void clear() { TASKS.clear(); }

    private Scheduler() {}
}

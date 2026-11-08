package io.github.vibernoslop;

import java.util.LinkedHashSet;
import de.robv.android.xposed.XposedBridge;

final class HookLog {
    private static final LinkedHashSet<String> entries = new LinkedHashSet<>();
    static synchronized void add(String message) {
        if (entries.add(message)) {
            XposedBridge.log("ViberNoSlop: " + message);
            if (entries.size() > 150) entries.remove(entries.iterator().next());
        }
    }
    static synchronized String report() { return String.join("\n", entries); }
}

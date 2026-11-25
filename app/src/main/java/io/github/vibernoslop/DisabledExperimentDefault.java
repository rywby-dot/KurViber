package io.github.vibernoslop;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/** Read the non-null stateless Disabled default captured by Viber's factory mapper. */
final class DisabledExperimentDefault {
    static Object find(Object wrapper) throws ReflectiveOperationException {
        Object match = null;
        for (Field captured : wrapper.getClass().getDeclaredFields()) {
            if (Modifier.isStatic(captured.getModifiers()) || captured.getType() != Object.class) continue;
            captured.setAccessible(true);
            Object fallback = captured.get(wrapper);
            if (fallback == null || fallback.getClass().getSuperclass() != Object.class
                || !"Disabled".equals(fallback.toString())) continue;
            boolean singleton = false, stateful = false;
            for (Field member : fallback.getClass().getDeclaredFields()) {
                if (!Modifier.isStatic(member.getModifiers())) { stateful = true; break; }
                if (member.getType() == fallback.getClass()) {
                    member.setAccessible(true);
                    if (member.get(null) == fallback) singleton = true;
                }
            }
            if (stateful || !singleton) continue;
            if (match != null && match != fallback)
                throw new NoSuchFieldException("Ambiguous Image Remix disabled default");
            match = fallback;
        }
        return match;
    }
    private DisabledExperimentDefault() {}
}

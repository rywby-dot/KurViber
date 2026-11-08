package io.github.vibernoslop;

import android.content.Context;
import android.content.SharedPreferences;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Set;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;

final class ExperimentHooks {
    static void install(Context context, ClassLoader loader) throws Exception {
        String apk = context.getApplicationInfo().sourceDir;
        File file = new File(apk);
        String identity = apk + ":" + file.length() + ":" + file.lastModified();
        SharedPreferences cache = context.getSharedPreferences("viber_noslop_discovery", 0);
        String type = identity.equals(cache.getString("identity", "")) ? cache.getString("experiment", null) : null;
        if (type == null) {
            Set<String> matches = DexFinder.find(apk, DexFinder.EXPERIMENT_ANCHOR);
            if (matches.size() != 1) {
                HookLog.add("AI experiment hook skipped: expected one fingerprint match, got " + matches.size());
                return;
            }
            type = matches.iterator().next();
        }
        Class<?> cls = Class.forName(type, false, loader);
        Method target = null;
        for (Method method : cls.getDeclaredMethods()) {
            Class<?>[] args = method.getParameterTypes();
            if (!Modifier.isStatic(method.getModifiers()) && method.getReturnType() == Object.class
                && args.length == 2 && !args[0].isPrimitive() && args[1] == boolean.class) {
                if (target != null) throw new IllegalStateException("Ambiguous experiment mapper");
                target = method;
            }
        }
        if (target == null) throw new IllegalStateException("Experiment mapper signature changed");
        XposedBridge.hookMethod(target, new XC_MethodHook() {
            @Override protected void beforeHookedMethod(MethodHookParam p) {
                if (!Options.on(Options.Option.AI)) return;
                try {
                    String key = findKey(p.thisObject);
                    // Each of these has a nullable disabled result in the inspected APK.
                    // Do not null arbitrary experiments: many have non-null domain models.
                    if ("core_ai_chatgptintegration".equals(key) || "core_ai_chatgpttab".equals(key)
                        || "core_ai_chatgpttabv2".equals(key)) {
                        p.setResult(null);
                        HookLog.add("AI experiment disabled: " + key);
                    }
                } catch (ReflectiveOperationException e) { HookLog.add("AI key lookup failed: " + e.getClass().getSimpleName()); }
            }
        });
        cache.edit().putString("identity", identity).putString("experiment", type).apply();
        HookLog.add("AI experiment mapper: " + target.toGenericString());
    }
    private static String findKey(Object mapper) throws IllegalAccessException {
        for (Field field : mapper.getClass().getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) || field.getType().isPrimitive()) continue;
            field.setAccessible(true);
            Object object = field.get(mapper);
            if (object == null || object.getClass().getClassLoader() != mapper.getClass().getClassLoader()) continue;
            for (Field nested : object.getClass().getDeclaredFields()) {
                if (Modifier.isStatic(nested.getModifiers()) || nested.getType() != String.class) continue;
                nested.setAccessible(true);
                String value = (String) nested.get(object);
                if (value != null && value.startsWith("core_ai_chatgpt")) return value;
            }
        }
        return null;
    }
}

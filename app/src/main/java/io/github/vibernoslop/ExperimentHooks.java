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
                if (!Options.on(Options.Option.AI) && !Options.on(Options.Option.POLISH_MESSAGE)
                    && !Options.on(Options.Option.IMAGE_REMIX)) return;
                try {
                    String key = findKey(p.thisObject);
                    Options.Option option = AiFeatureRules.experiment(key);
                    if (option == null || !Options.on(option)) return;
                    // ChatGPT and Writing Assistant explicitly use null as disabled.
                    // Image Remix requires the factory's non-null Disabled default.
                    Object disabled = option == Options.Option.IMAGE_REMIX
                        ? remixDefault(p.thisObject) : null;
                    p.setResult(disabled);
                    HookLog.add("AI experiment disabled: " + key);
                } catch (ReflectiveOperationException e) { HookLog.add("AI experiment override skipped: " + e.getClass().getSimpleName() + ": " + e.getMessage()); }
            }
        });
        cache.edit().putString("identity", identity).putString("experiment", type).apply();
        HookLog.add("AI experiment mapper: " + target.toGenericString());
    }
    private static Object remixDefault(Object mapper) throws ReflectiveOperationException {
        Object match = null;
        for (Field field : mapper.getClass().getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) || field.getType().isPrimitive()) continue;
            field.setAccessible(true);
            Object wrapper = field.get(mapper);
            if (wrapper == null) continue;
            boolean function = false;
            for (Class<?> iface : wrapper.getClass().getInterfaces())
                if (iface.getName().equals("kotlin.jvm.functions.Function1")) function = true;
            if (!function) continue;
            Object fallback = DisabledExperimentDefault.find(wrapper);
            if (fallback == null) continue;
            if (match != null && match != fallback)
                throw new NoSuchFieldException("Ambiguous Image Remix disabled default");
            match = fallback;
        }
        if (match == null) throw new NoSuchFieldException("Image Remix disabled default signature changed");
        return match;
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
                if (AiFeatureRules.experiment(value) != null) return value;
            }
        }
        return null;
    }
}

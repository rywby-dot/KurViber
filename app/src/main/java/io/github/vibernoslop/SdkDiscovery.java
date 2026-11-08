package io.github.vibernoslop;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.IInterface;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;

final class SdkDiscovery {
    static final String BRAZE = "Setting Braze SDK disabled to: ";
    static final String CRASH = "Crashlytics automatic data collection ";
    static final String GOOGLE = "Failed to load ad.";
    static void install(Context context, ClassLoader loader) throws Exception {
        String apk = context.getApplicationInfo().sourceDir;
        File file = new File(apk);
        String identity = apk + ":" + file.length() + ":" + file.lastModified();
        SharedPreferences cache = context.getSharedPreferences("viber_noslop_sdk_discovery", 0);
        String[] anchors = {BRAZE, CRASH, GOOGLE};
        Map<String, Set<String>> found = new java.util.LinkedHashMap<>();
        boolean cached = identity.equals(cache.getString("identity", ""));
        if (cached) for (String key : anchors) {
            Set<String> values = cache.getStringSet(key, null);
            if (values == null) { cached = false; break; }
            found.put(key, values);
        }
        if (!cached) {
            found = DexFinder.findAll(apk, anchors);
            SharedPreferences.Editor edit = cache.edit().clear().putString("identity", identity);
            for (String key : anchors) edit.putStringSet(key, found.get(key));
            edit.apply();
        }
        gate(found.get(BRAZE), loader, Options.Option.ANALYTICS, true, "Braze disabled state");
        gate(found.get(CRASH), loader, Options.Option.CRASHES, false, "Crashlytics collection arbiter");
        google(found.get(GOOGLE), loader);
    }
    private static void gate(Set<String> types, ClassLoader loader, Options.Option option, boolean value, String label) {
        try {
            if (types.size() != 1) throw new IllegalStateException("anchor matches=" + types.size());
            Class<?> cls = Class.forName(types.iterator().next(), false, loader);
            Method target = null;
            for (Method method : cls.getDeclaredMethods()) {
                if (Modifier.isStatic(method.getModifiers()) || method.getReturnType() != boolean.class || method.getParameterCount() != 0) continue;
                if (target != null) throw new IllegalStateException("multiple Boolean getters");
                target = method;
            }
            if (target == null) throw new NoSuchMethodException("Boolean getter");
            hook(target, option, value);
            HookLog.add(label + ": " + target.toGenericString());
        } catch (Throwable e) { HookLog.add(label + " skipped: " + e.getMessage()); }
    }
    private static void google(Set<String> types, ClassLoader loader) {
        try {
            Method load = null;
            for (String type : types) {
                Class<?> cls = Class.forName(type, false, loader);
                boolean context = false, binder = false;
                for (Field field : cls.getDeclaredFields()) {
                    if (Modifier.isStatic(field.getModifiers())) continue;
                    context |= field.getType() == Context.class;
                    binder |= IInterface.class.isAssignableFrom(field.getType());
                }
                if (!context || !binder) continue;
                for (Method method : cls.getDeclaredMethods()) {
                    Class<?>[] args = method.getParameterTypes();
                    if (Modifier.isStatic(method.getModifiers()) || method.getReturnType() != void.class
                        || args.length != 1 || args[0].isPrimitive()) continue;
                    if (load != null) throw new IllegalStateException("multiple ad loading candidates");
                    load = method;
                }
            }
            if (load == null) throw new NoSuchMethodException("Google native ad request");
            hook(load, Options.Option.AD_REQUESTS, null);
            HookLog.add("Google native ad load: " + load.toGenericString());
            Class<?> request = load.getParameterTypes()[0];
            Class<?> banner = Class.forName("com.google.android.gms.ads.BaseAdView", false, loader);
            int count = 0;
            for (Method method : banner.getDeclaredMethods()) {
                Class<?>[] args = method.getParameterTypes();
                if (method.getReturnType() != void.class || args.length != 1 || args[0].isPrimitive()
                    || Modifier.isStatic(method.getModifiers()) || !Modifier.isFinal(method.getModifiers())) continue;
                boolean requestWrapper = args[0] == request;
                for (Field field : args[0].getDeclaredFields())
                    if (!Modifier.isStatic(field.getModifiers()) && field.getType() == request) requestWrapper = true;
                if (requestWrapper) { hook(method, Options.Option.AD_REQUESTS, null); count++; }
            }
            HookLog.add("Google banner load fingerprint: " + count + " hooks");
        } catch (Throwable e) { HookLog.add("Google ad discovery skipped: " + e.getMessage()); }
    }
    private static void hook(Method method, Options.Option option, Object result) {
        XposedBridge.hookMethod(method, new XC_MethodHook() {
            @Override protected void beforeHookedMethod(MethodHookParam p) { if (Options.on(option)) p.setResult(result); }
        });
    }
}

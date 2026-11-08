package io.github.vibernoslop;

import android.content.Context;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;

/** Remove the ad from adapter counts and position mapping, before any row is created. */
final class AdAdapterHooks {
    private static final Map<Object, Options.Option> policies = Collections.synchronizedMap(new WeakHashMap<>());
    static void install(Context context, ClassLoader loader) throws Exception {
        Class<?> more = Class.forName("com.viber.voip.user.more.PreferencesWithAdsRecyclerAdapterWrapper", false, loader);
        Class<?> base = more.getSuperclass();
        Method available = base.getDeclaredMethod("isAdAvailable");
        Method count = base.getDeclaredMethod("getItemCount");
        Method position = base.getDeclaredMethod("getAdPosition");
        if (available.getReturnType() != boolean.class || count.getReturnType() != int.class
            || position.getReturnType() != int.class) throw new IllegalStateException("Ad adapter signature changed");
        XposedBridge.hookMethod(available, new XC_MethodHook() {
            @Override protected void beforeHookedMethod(MethodHookParam p) {
                Options.Option rule = policy(context, p.thisObject, base, more);
                if (rule != null && Options.on(rule)) p.setResult(false);
            }
        });
        try {
            XposedBridge.hookMethod(base.getDeclaredMethod("notifyAdChanged"), new XC_MethodHook() {
                @Override protected void beforeHookedMethod(MethodHookParam p) {
                    Options.Option rule = policy(context, p.thisObject, base, more);
                    if (rule != null && Options.on(rule)) p.setResult(null);
                }
            });
        } catch (NoSuchMethodException ignored) { HookLog.add("Optional notifyAdChanged hook missing"); }
        HookLog.add("Ad adapter row removal: " + base.getName());
    }
    private static Options.Option policy(Context context, Object adapter, Class<?> base, Class<?> more) {
        Options.Option cached = policies.get(adapter);
        if (cached != null) return cached;
        if (more.isInstance(adapter)) {
            policies.put(adapter, Options.Option.ADS_MORE);
            return Options.Option.ADS_MORE;
        }
        try {
            // Identify the ad layout by its resource name, regardless of field name or integer ID.
            for (Field field : base.getDeclaredFields()) {
                if (field.getType() != int.class || Modifier.isStatic(field.getModifiers())) continue;
                field.setAccessible(true);
                Options.Option rule = UiRules.layout(UiHooks.name(context, field.getInt(adapter)));
                if (rule == Options.Option.ADS_CHATS || rule == Options.Option.ADS_MORE || rule == Options.Option.ADS_OTHER) {
                    policies.put(adapter, rule);
                    HookLog.add("Ad adapter policy: " + adapter.getClass().getName() + " / " + rule);
                    return rule;
                }
            }
        } catch (ReflectiveOperationException e) { HookLog.add("Ad adapter policy skipped: " + e.getClass().getSimpleName()); }
        return null;
    }
}

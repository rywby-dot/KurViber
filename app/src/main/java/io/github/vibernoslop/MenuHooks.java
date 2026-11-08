package io.github.vibernoslop;

import android.content.Context;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuInflater;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;

final class MenuHooks {
    private static final Set<MenuItem> aiItems = Collections.newSetFromMap(Collections.synchronizedMap(new WeakHashMap<>()));
    static void install(Context context, ClassLoader loader) {
        for (String type : new String[]{"android.view.MenuInflater", "androidx.appcompat.view.SupportMenuInflater"}) {
            try {
                Class<?> cls = Class.forName(type, false, loader);
                XposedBridge.hookMethod(cls.getDeclaredMethod("inflate", int.class, Menu.class), new XC_MethodHook() {
                    @Override protected void afterHookedMethod(MethodHookParam p) {
                        if (!p.hasThrowable()) filter(context, (Menu) p.args[1]);
                    }
                });
            } catch (Throwable e) { HookLog.add("Menu inflater unavailable: " + type); }
        }
        for (String type : new String[]{"androidx.appcompat.view.menu.MenuItemImpl", "com.android.internal.view.menu.MenuItemImpl"}) {
            try {
                Class<?> cls = Class.forName(type, false, loader);
                XposedBridge.hookMethod(cls.getDeclaredMethod("setVisible", boolean.class), new XC_MethodHook() {
                    @Override protected void beforeHookedMethod(MethodHookParam p) {
                        if (aiItems.contains(p.thisObject)) p.args[0] = false;
                    }
                });
            } catch (Throwable e) { HookLog.add("Menu visibility hook unavailable: " + type); }
        }
    }
    private static void filter(Context context, Menu menu) {
        for (int i = 0; i < menu.size(); i++) {
            MenuItem item = menu.getItem(i);
            if ("menu_camera".equals(UiHooks.name(context, item.getItemId())) && Options.on(Options.Option.CAMERA)) { aiItems.add(item); item.setVisible(false); }
            if (UiRules.id(UiHooks.name(context, item.getItemId())) == Options.Option.AI) {
                if (Options.on(Options.Option.AI)) aiItems.add(item);
                if (Options.on(Options.Option.AI)) item.setVisible(false);
            }
            if (item.hasSubMenu()) filter(context, item.getSubMenu());
        }
    }
}

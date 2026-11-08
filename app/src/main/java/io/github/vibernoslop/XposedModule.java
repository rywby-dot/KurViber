package io.github.vibernoslop;

import android.app.Application;
import android.content.Context;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public final class XposedModule implements IXposedHookLoadPackage {
    private static boolean installed;
    @Override public void handleLoadPackage(XC_LoadPackage.LoadPackageParam pkg) {
        if (!"com.viber.voip".equals(pkg.packageName)) return;
        XposedHelpers.findAndHookMethod(Application.class, "attach", Context.class, new XC_MethodHook() {
            @Override protected void beforeHookedMethod(MethodHookParam p) {
                synchronized (XposedModule.class) {
                    if (installed) return;
                    installed = true;
                }
                Context context = (Context) p.args[0];
                Options.init(context);
                HookLog.add("Build 0.2.3 / toolbar diagnostics revision 1");
                for (Options.Option option : Options.Option.values()) HookLog.add("Option " + option.name() + "=" + Options.on(option));
                HookLog.add("Loaded in " + pkg.processName);
                ClassLoader loader = pkg.classLoader;
                safe("tracker DNS", TrackerNetwork::install);
                safe("metadata", MetadataHooks::install);
                safe("SDK discovery", () -> SdkDiscovery.install(context, loader));
                safe("privacy", () -> new PrivacyHooks(loader).install());
                if ("com.viber.voip".equals(pkg.processName)) {
                    safe("ad adapters", () -> AdAdapterHooks.install(context, loader));
                    safe("appearance", () -> Appearance.install(loader));
                    safe("UI", () -> UiHooks.install(loader));
                    safe("menus", () -> MenuHooks.install(context, loader));
                    safe("settings", () -> SettingsEntry.install(loader));
                    safe("AI discovery", () -> ExperimentHooks.install(context, loader));
                }
            }
        });
    }
    interface Action { void run() throws Exception; }
    private static void safe(String label, Action action) {
        try { action.run(); }
        catch (Throwable e) { HookLog.add(label + " skipped: " + e.getClass().getSimpleName() + ": " + e.getMessage()); }
    }
}

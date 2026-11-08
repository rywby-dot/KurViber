package io.github.vibernoslop;

import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;

final class MetadataHooks {
    static void install() throws ClassNotFoundException {
        Class<?> manager = Class.forName("android.app.ApplicationPackageManager");
        XposedBridge.hookAllMethods(manager, "getApplicationInfo", new XC_MethodHook() {
            @Override protected void afterHookedMethod(MethodHookParam p) {
                if (!(p.getResult() instanceof ApplicationInfo)) return;
                ApplicationInfo original = (ApplicationInfo) p.getResult();
                if (!"com.viber.voip".equals(original.packageName)) return;
                ApplicationInfo copy = new ApplicationInfo(original);
                copy.metaData = original.metaData == null ? new Bundle() : new Bundle(original.metaData);
                if (Options.on(Options.Option.ANALYTICS)) {
                    copy.metaData.putBoolean("firebase_analytics_collection_deactivated", true);
                    copy.metaData.putBoolean("firebase_analytics_collection_enabled", false);
                }
                if (Options.on(Options.Option.CRASHES)) copy.metaData.putBoolean("firebase_crashlytics_collection_enabled", false);
                p.setResult(copy);
            }
        });
        HookLog.add("Firebase metadata overrides installed");
    }
}

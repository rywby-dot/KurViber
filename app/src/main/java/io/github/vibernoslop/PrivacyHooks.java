package io.github.vibernoslop;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import static io.github.vibernoslop.Options.Option.*;

final class PrivacyHooks {
    private final ClassLoader loader;
    PrivacyHooks(ClassLoader loader) { this.loader = loader; }
    void block(String name, Options.Option option, Class<?> returns, Object value, String... names) {
        try {
            Class<?> cls = Class.forName(name, false, loader);
            Set<String> wanted = new HashSet<>(Arrays.asList(names));
            int count = 0;
            for (Method method : cls.getDeclaredMethods()) {
                if (!wanted.contains(method.getName()) || method.getReturnType() != returns
                    || Modifier.isAbstract(method.getModifiers())) continue;
                XposedBridge.hookMethod(method, new XC_MethodHook() {
                    @Override protected void beforeHookedMethod(MethodHookParam p) {
                        if (Options.on(option)) p.setResult(value);
                    }
                });
                count++;
            }
            HookLog.add(option + ": " + name + " (" + count + " hooks)");
        } catch (Throwable e) { HookLog.add("Unavailable: " + name + " / " + e.getClass().getSimpleName()); }
    }
    void install() {
        for (String name : new String[]{"com.adjust.sdk.Adjust", "com.adjust.sdk.AdjustInstance"})
            block(name, ANALYTICS, void.class, null, "initSdk", "onResume", "onPause", "enable", "trackEvent",
                "trackAdRevenue", "trackMeasurementConsent", "trackThirdPartySharing", "trackPlayStoreSubscription",
                "setPushToken", "setReferrer", "sendReferrer", "sendPreinstallReferrer");
        String mixpanel = "com.mixpanel.android.mpmetrics.MixpanelAPI";
        block(mixpanel, ANALYTICS, boolean.class, true, "hasOptedOutTracking");
        block(mixpanel, ANALYTICS, void.class, null, "track", "trackMap", "trackWithGroups", "flush", "identify",
            "alias", "optInTracking", "recordPeopleMessage", "recordGroupMessage", "registerMixpanelActivityLifecycleCallbacks");
        block("com.braze.Braze", ANALYTICS, void.class, null, "logCustomEvent", "openSession", "closeSession", "changeUser", "requestImmediateDataFlush");
        braze();
        block("com.google.firebase.analytics.FirebaseAnalytics", ANALYTICS, void.class, null,
            "logEvent", "setUserId", "setUserProperty", "setCurrentScreen", "setDefaultEventParameters");
        block("com.google.firebase.crashlytics.FirebaseCrashlytics", CRASHES, boolean.class, false, "isCrashlyticsCollectionEnabled");
        block("com.google.firebase.crashlytics.FirebaseCrashlytics", CRASHES, void.class, null,
            "log", "recordException", "sendUnsentReports", "setUserId", "setCustomKey", "setCustomKeys");
        forceFalseArgument("com.google.firebase.crashlytics.FirebaseCrashlytics", "setCrashlyticsCollectionEnabled", CRASHES);
        forceFalseArgument("com.google.firebase.analytics.FirebaseAnalytics", "setAnalyticsCollectionEnabled", ANALYTICS);
        block("com.google.firebase.crashlytics.internal.common.DataCollectionArbiter", CRASHES, boolean.class, false, "isAutomaticDataCollectionEnabled");
        block("com.google.android.gms.ads.AdLoader", AD_REQUESTS, void.class, null, "loadAd", "loadAds");
        block("com.google.android.gms.ads.BaseAdView", AD_REQUESTS, void.class, null, "loadAd");
        block("com.google.android.gms.ads.admanager.AdManagerAdView", AD_REQUESTS, void.class, null, "loadAd");
        block("com.google.android.gms.ads.interstitial.InterstitialAd", AD_REQUESTS, void.class, null, "load");
        block("com.google.android.gms.ads.appopen.AppOpenAd", AD_REQUESTS, void.class, null, "load");
        block("com.rakuten.rmp.mobile.InterstitialAd", AD_REQUESTS, void.class, null, "loadAd");
        block("com.rakuten.rmp.mobile.VideoAd", AD_REQUESTS, void.class, null, "loadAdForPlayerView", "playAd");
        block("com.rakuten.rmp.mobile.AdUnit", AD_REQUESTS, void.class, null, "handleClicked", "handleImpression");
        rakutenRequests();
        block("com.viber.voip.core.ads.deviceinfo.AdvertisingIdClientUtils", ANALYTICS, String.class, "", "fetchAndUpdateAdInfo");
        block("com.viber.jni.cdr.CdrController", VIBER_EVENTS, boolean.class, true, "handleReportCdr");
        block("com.viber.jni.cdr.CdrController", ANALYTICS, void.class, null, "setAdvertisingId");
        dataSdk();
    }
    private void forceFalseArgument(String name, String methodName, Options.Option option) {
        try {
            for (Method method : Class.forName(name, false, loader).getDeclaredMethods()) {
                Class<?>[] args = method.getParameterTypes();
                if (!method.getName().equals(methodName) || args.length != 1
                    || (args[0] != boolean.class && args[0] != Boolean.class)) continue;
                XposedBridge.hookMethod(method, new XC_MethodHook() {
                    @Override protected void beforeHookedMethod(MethodHookParam p) { if (Options.on(option)) p.args[0] = false; }
                });
            }
        } catch (Throwable e) { HookLog.add("Collection setter unavailable: " + name); }
    }
    private void dataSdk() {
        try {
            Class<?> cls = Class.forName("com.viber.voip.core.cdranalytics.datasdk.config.DataSdkGbExperimentProvider$SerializableModel", false, loader);
            int count = 0;
            for (Constructor<?> constructor : cls.getDeclaredConstructors()) {
                Class<?>[] args = constructor.getParameterTypes();
                // Kotlin primary constructor: six numeric/config fields followed by two nullable flags.
                if (args.length != 8 || args[0] != String.class || args[6] != Boolean.class || args[7] != Boolean.class) continue;
                XposedBridge.hookMethod(constructor, new XC_MethodHook() {
                    @Override protected void beforeHookedMethod(MethodHookParam p) {
                        if (Options.on(VIBER_EVENTS)) { p.args[6] = false; p.args[7] = false; }
                    }
                });
                count++;
            }
            // Deserialization constructors may bypass primary constructor. Only this model's
            // two Boolean fields represent directOutput/dataSdkOutput, verified against APK.
            Field[] flags = Arrays.stream(cls.getDeclaredFields())
                .filter(f -> !Modifier.isStatic(f.getModifiers()) && f.getType() == Boolean.class).toArray(Field[]::new);
            if (flags.length != 2) throw new IllegalStateException("Data SDK model changed");
            for (Field field : flags) field.setAccessible(true);
            XposedBridge.hookAllConstructors(cls, new XC_MethodHook() {
                @Override protected void afterHookedMethod(MethodHookParam p) throws Throwable {
                    if (Options.on(VIBER_EVENTS) && !p.hasThrowable()) for (Field f : flags) f.set(p.thisObject, Boolean.FALSE);
                }
            });
            HookLog.add("Viber data SDK outputs disabled (model fingerprint)");
        } catch (Throwable e) { HookLog.add("Data SDK hook skipped: " + e.getClass().getSimpleName()); }
    }
    private void braze() {
        try {
            Class<?> cls = Class.forName("com.braze.Braze", false, loader);
            Field offline = cls.getDeclaredField("areOutboundNetworkRequestsOffline");
            Field drop = cls.getDeclaredField("shouldMockNetworkRequestsAndDropEvents");
            offline.setAccessible(true); drop.setAccessible(true);
            XposedBridge.hookAllConstructors(cls, new XC_MethodHook() {
                @Override protected void beforeHookedMethod(MethodHookParam p) throws Throwable {
                    if (Options.on(ANALYTICS)) { offline.setBoolean(null, true); drop.setBoolean(null, true); }
                }
            });
            for (Method method : cls.getDeclaredMethods()) {
                if (method.getName().equals("access$setAreOutboundNetworkRequestsOffline$cp")
                    || method.getName().equals("access$setShouldMockNetworkRequestsAndDropEvents$cp"))
                    if (Arrays.equals(method.getParameterTypes(), new Class<?>[]{boolean.class}))
                        XposedBridge.hookMethod(method, new XC_MethodHook() {
                            @Override protected void beforeHookedMethod(MethodHookParam p) { if (Options.on(ANALYTICS)) p.args[0] = true; }
                        });
            }
            HookLog.add("Braze offline/drop-events hooks installed");
        } catch (Throwable e) { HookLog.add("Braze offline hook skipped: " + e.getClass().getSimpleName()); }
    }
    private void rakutenRequests() {
        try {
            Class<?> unit = Class.forName("com.rakuten.rmp.mobile.AdUnit", false, loader);
            Class<?> callback = Class.forName("com.rakuten.rmp.mobile.listeners.OnCompleteListener", false, loader);
            int count = 0;
            for (Method method : unit.getDeclaredMethods()) {
                if (method.getReturnType() != void.class || !Arrays.equals(method.getParameterTypes(), new Class<?>[]{callback})) continue;
                Class<?> resultCode = Class.forName("com.rakuten.rmp.mobile.ResultCode", false, loader);
                Object noBids = resultCode.getField("NO_BIDS").get(null);
                Method complete = null;
                for (Method m : callback.getMethods()) {
                    Class<?>[] parameters = m.getParameterTypes();
                    if (m.getName().equals("onComplete") && parameters.length == 3 && parameters[0] == resultCode)
                        complete = m;
                }
                if (complete == null) throw new NoSuchMethodException("Rakuten completion callback");
                final Method completion = complete;
                XposedBridge.hookMethod(method, new XC_MethodHook() {
                    @Override protected void beforeHookedMethod(MethodHookParam p) {
                        if (!Options.on(AD_REQUESTS)) return;
                        p.setResult(null);
                        Object listener = p.args[0];
                        if (listener != null) new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                            try { completion.invoke(listener, noBids, null, null); }
                            catch (Throwable e) { HookLog.add("Rakuten no-bid callback failed: " + e.getClass().getSimpleName()); }
                        });
                    }
                });
                count++;
            }
            HookLog.add("Rakuten request entry points: " + count);
        } catch (Throwable e) { HookLog.add("Rakuten hook skipped: " + e.getClass().getSimpleName()); }
    }
}

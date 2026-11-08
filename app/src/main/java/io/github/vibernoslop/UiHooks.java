package io.github.vibernoslop;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Rect;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

final class UiHooks {
    private static final Map<View, Options.Option> marked = Collections.synchronizedMap(new WeakHashMap<>());
    private static final ThreadLocal<Boolean> applying = ThreadLocal.withInitial(() -> false);
    private static final Map<View, Boolean> launchers = Collections.synchronizedMap(new WeakHashMap<>());

    static String name(View view) { return name(view.getContext(), view.getId()); }
    static String name(Context context, int id) {
        if (id <= 0) return "";
        try { return context.getResources().getResourceEntryName(id); }
        catch (android.content.res.Resources.NotFoundException ignored) { return ""; }
    }
    static boolean hidden(View view) {
        Options.Option option = marked.get(view);
        return option != null && Options.on(option);
    }
    private static void zero(ViewGroup.LayoutParams lp) {
        if (lp == null) return;
        lp.height = 0;
        if (lp instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams m = (ViewGroup.MarginLayoutParams) lp;
            m.setMargins(0, 0, 0, 0);
            m.setMarginStart(0); m.setMarginEnd(0);
        }
        if (lp instanceof LinearLayout.LayoutParams) ((LinearLayout.LayoutParams) lp).weight = 0;
    }
    static void mark(View view, Options.Option option) {
        if (view == null || option == null || view instanceof android.view.ViewStub) return;
        marked.put(view, option);
        if (!Options.on(option)) return;
        applying.set(true);
        try {
            view.setVisibility(View.GONE);
            view.setMinimumHeight(0);
            view.setPadding(0, 0, 0, 0);
            ViewGroup.LayoutParams lp = view.getLayoutParams();
            zero(lp);
            if (lp != null) view.setLayoutParams(lp);
            HookLog.add("UI: " + option.name() + " / " + name(view));
        } finally { applying.set(false); }
    }
    static Activity activity(Context context) {
        for (int n = 0; n < 12 && context != null; n++) {
            if (context instanceof Activity) return (Activity) context;
            if (!(context instanceof ContextWrapper)) return null;
            Context next = ((ContextWrapper) context).getBaseContext();
            if (next == context) return null;
            context = next;
        }
        return null;
    }
    static void scan(View view) {
        if (view == null) return;
        String id = name(view);
        HomeIconColors.apply(view);
        Options.Option option = UiRules.id(id);
        if (option != null && !marked.containsKey(view)) mark(view, option);
        if ("activity_home_toolbar".equals(id) && !launchers.containsKey(view)) {
            Activity host = activity(view.getContext());
            if (host != null) {
                HomeControls.install(view, host);
                view.setOnLongClickListener(v -> { SettingsUi.show(host); return true; });
                launchers.put(view, true);
                HookLog.add("Settings: home toolbar long press installed");
            }
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) scan(group.getChildAt(i));
        }
    }
    static void install(ClassLoader loader) {
        XposedHelpers.findAndHookMethod(LayoutInflater.class, "inflate", int.class, ViewGroup.class, boolean.class, new XC_MethodHook() {
            @Override protected void afterHookedMethod(MethodHookParam p) {
                if (p.hasThrowable()) return;
                View result = (View) p.getResult();
                if (result == null) return;
                LayoutInflater inflater = (LayoutInflater) p.thisObject;
                Options.Option rule = UiRules.layout(name(inflater.getContext(), (int) p.args[0]));
                ViewGroup parent = (ViewGroup) p.args[1];
                // With attachToRoot, the inflater returns the parent, not the new child.
                if (rule != null) {
                    if (result != parent) mark(result, rule);
                    else if (parent != null && parent.getChildCount() > 0)
                        mark(parent.getChildAt(parent.getChildCount() - 1), rule);
                }
                scan(result);
            }
        });
        XposedHelpers.findAndHookMethod(View.class, "setId", int.class, new XC_MethodHook() {
            @Override protected void afterHookedMethod(MethodHookParam p) {
                View view = (View) p.thisObject;
                Options.Option rule = UiRules.id(name(view));
                if (rule != null) mark(view, rule);
            }
        });
        XposedHelpers.findAndHookMethod(ViewGroup.class, "addView", View.class, int.class, ViewGroup.LayoutParams.class, new XC_MethodHook() {
            @Override protected void afterHookedMethod(MethodHookParam p) { if (!p.hasThrowable()) scan((View) p.args[0]); }
        });
        XposedHelpers.findAndHookMethod(View.class, "setVisibility", int.class, new XC_MethodHook() {
            @Override protected void beforeHookedMethod(MethodHookParam p) {
                if (hidden((View) p.thisObject)) p.args[0] = View.GONE;
            }
        });
        XposedHelpers.findAndHookMethod(View.class, "setLayoutParams", ViewGroup.LayoutParams.class, new XC_MethodHook() {
            @Override protected void beforeHookedMethod(MethodHookParam p) {
                if (!applying.get() && hidden((View) p.thisObject)) zero((ViewGroup.LayoutParams) p.args[0]);
            }
        });
        // RecyclerView measures even GONE item roots. Enforce zero height at measurement,
        // including widgets that use a fixed height in their own onMeasure implementation.
        XposedHelpers.findAndHookMethod(View.class, "setMeasuredDimension", int.class, int.class, new XC_MethodHook() {
            @Override protected void beforeHookedMethod(MethodHookParam p) {
                if (hidden((View) p.thisObject)) p.args[1] = 0;
            }
        });
        try {
            Class<?> recycler = Class.forName("androidx.recyclerview.widget.RecyclerView", false, loader);
            java.lang.reflect.Method candidate = null;
            for (java.lang.reflect.Method method : recycler.getDeclaredMethods()) {
                if (method.getReturnType() == Rect.class && java.util.Arrays.equals(method.getParameterTypes(), new Class<?>[]{View.class})) {
                    if (candidate != null) throw new IllegalStateException("Ambiguous decoration method");
                    candidate = method;
                }
            }
            if (candidate == null) throw new NoSuchMethodException("Rect(View)");
            XposedBridge.hookMethod(candidate, new XC_MethodHook() {
                @Override protected void afterHookedMethod(MethodHookParam p) {
                    if (hidden((View) p.args[0]) && p.getResult() instanceof Rect) ((Rect) p.getResult()).setEmpty();
                }
            });
            HookLog.add("RecyclerView zero-insets hook installed");
        } catch (Throwable error) { HookLog.add("Optional RecyclerView decoration hook unavailable: " + error.getClass().getSimpleName()); }
        XposedHelpers.findAndHookMethod(Activity.class, "onResume", new XC_MethodHook() {
            @Override protected void afterHookedMethod(MethodHookParam p) {
                Activity a = (Activity) p.thisObject;
                a.getWindow().getDecorView().post(() -> scan(a.getWindow().getDecorView()));
            }
        });
        HookLog.add("UI hooks installed");
    }
}

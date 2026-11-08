package io.github.vibernoslop;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

final class SettingsEntry {
    static void install(ClassLoader loader) {
        XposedHelpers.findAndHookMethod(Activity.class, "onPostResume", new XC_MethodHook() {
            @Override protected void afterHookedMethod(MethodHookParam p) {
                Activity activity = (Activity) p.thisObject;
                if (!activity.getClass().getName().equals("com.viber.voip.settings.ui.SettingsHeadersActivity")) return;
                activity.getWindow().getDecorView().post(() -> add(activity));
            }
        });
    }
    private static void add(Activity activity) {
        try {
            int id = activity.getResources().getIdentifier("settingsFragmentHolder", "id", "com.viber.voip");
            View holder = activity.findViewById(id);
            if (holder == null || !(holder.getParent() instanceof ViewGroup)) return;
            ViewGroup parent = (ViewGroup) holder.getParent();
            if ("viber_noslop_settings_wrapper".equals(parent.getTag())) return;
            int index = parent.indexOfChild(holder);
            ViewGroup.LayoutParams original = holder.getLayoutParams();
            LinearLayout wrapper = new LinearLayout(activity);
            wrapper.setTag("viber_noslop_settings_wrapper");
            wrapper.setOrientation(LinearLayout.VERTICAL);
            TextView entry = new TextView(activity);
            entry.setText("Viber NoSlop  ›");
            entry.setTextSize(18);
            int pad = (int) (20 * activity.getResources().getDisplayMetrics().density);
            entry.setPadding(pad, pad, pad, pad);
            android.util.TypedValue background = new android.util.TypedValue();
            if (activity.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, background, true))
                entry.setBackgroundResource(background.resourceId);
            entry.setOnClickListener(v -> SettingsUi.show(activity));
            parent.removeView(holder);
            wrapper.addView(entry, new LinearLayout.LayoutParams(-1, -2));
            wrapper.addView(holder, new LinearLayout.LayoutParams(-1, 0, 1));
            parent.addView(wrapper, index, original);
            HookLog.add("Settings: Viber NoSlop section inserted");
        } catch (Throwable e) { HookLog.add("Settings entry unavailable: " + e.getClass().getSimpleName()); }
    }
}

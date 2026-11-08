package io.github.vibernoslop;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

final class SettingsUi {
    static void show(Activity activity) {
        LinearLayout content = new LinearLayout(activity);
        content.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (20 * activity.getResources().getDisplayMetrics().density);
        content.setPadding(pad, pad / 2, pad, pad);
        TextView hint = new TextView(activity);
        hint.setText("Changes apply after force-stopping and reopening Viber. Long-press the home toolbar for module settings.");
        content.addView(hint);
        for (Options.Option option : Options.Option.values()) {
            Switch item = new Switch(activity);
            item.setText(option.title);
            item.setTextSize(16);
            item.setPadding(0, pad / 2, 0, pad / 2);
            item.setChecked(Options.saved(option));
            item.setOnCheckedChangeListener((view, checked) -> Options.set(option, checked));
            content.addView(item);
        }
        android.widget.EditText title = new android.widget.EditText(activity);
        title.setHint("Custom app title (empty = original)");
        title.setText(Options.text("title", Options.DEFAULT_TITLE));
        content.addView(title);
        android.widget.EditText accent = new android.widget.EditText(activity);
        accent.setHint("Accent color: #RRGGBB");
        accent.setText(Options.text("accent", Options.DEFAULT_ACCENT));
        content.addView(accent);
        ScrollView scroll = new ScrollView(activity);
        scroll.addView(content);
        new AlertDialog.Builder(activity).setTitle("Viber NoSlop")
            .setView(scroll).setPositiveButton("Done", (d, w) -> {
                Options.setText("title", title.getText().toString());
                try { Color.parseColor(accent.getText().toString()); Options.setText("accent", accent.getText().toString()); }
                catch (IllegalArgumentException e) { Toast.makeText(activity, "Invalid color", Toast.LENGTH_SHORT).show(); }
            })
            .setNeutralButton("Diagnostics", (dialog, which) -> {
                String report = "Viber NoSlop 0.2.3\n" + HookLog.report() + "\n" + RuntimeDiagnostics.report(activity);
                new AlertDialog.Builder(activity).setTitle("Hook diagnostics")
                    .setMessage(report).setPositiveButton("Close", null)
                    .setNeutralButton("Copy", (d, w) -> {
                        ClipboardManager cb = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
                        cb.setPrimaryClip(ClipData.newPlainText("Viber NoSlop", report));
                        Toast.makeText(activity, "Copied", Toast.LENGTH_SHORT).show();
                    }).show();
            }).show();
    }
}

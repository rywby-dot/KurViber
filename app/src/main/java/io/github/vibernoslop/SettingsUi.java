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
        for (boolean enhancement : new boolean[] {false, true}) {
            TextView heading = new TextView(activity);
            heading.setText(enhancement ? "Enable" : "Disable");
            heading.setTextSize(18);
            heading.setPadding(0, pad, 0, pad / 2);
            content.addView(heading);
            for (Options.Option option : Options.Option.values()) {
                if (option.isEnhancement() != enhancement) continue;
                Switch item = new Switch(activity);
                item.setText(option.title);
                item.setTextSize(16);
                item.setPadding(0, pad / 2, 0, pad / 2);
                item.setChecked(Options.saved(option));
                item.setOnCheckedChangeListener((view, checked) -> Options.set(option, checked));
                content.addView(item);
            }
        }
        android.widget.EditText title = new android.widget.EditText(activity);
        title.setHint("Custom app title (empty = original)");
        title.setText(Options.text("title", Options.DEFAULT_TITLE));
        content.addView(title);
        ScrollView scroll = new ScrollView(activity);
        scroll.addView(content);
        new AlertDialog.Builder(activity).setTitle("KurViber")
            .setView(scroll).setPositiveButton("Done", (d, w) -> {
                Options.setText("title", title.getText().toString());
            })
            .setNeutralButton("Diagnostics", (dialog, which) -> {
                String report = "KurViber 0.5.3\n" + HookLog.report() + "\n" + RuntimeDiagnostics.report(activity);
                new AlertDialog.Builder(activity).setTitle("Hook diagnostics")
                    .setMessage(report).setPositiveButton("Close", null)
                    .setNeutralButton("Copy", (d, w) -> {
                        ClipboardManager cb = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
                        cb.setPrimaryClip(ClipData.newPlainText("KurViber", report));
                        Toast.makeText(activity, "Copied", Toast.LENGTH_SHORT).show();
                    }).show();
            }).show();
    }
}

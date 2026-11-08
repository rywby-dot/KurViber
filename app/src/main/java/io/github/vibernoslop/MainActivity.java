package io.github.vibernoslop;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public final class MainActivity extends Activity {
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        TextView info = new TextView(this);
        int p = (int) (24 * getResources().getDisplayMetrics().density);
        info.setPadding(p, p, p, p);
        info.setTextSize(18);
        info.setText("Viber NoSlop\n\nEnable this module in Vector with Viber as its scope. Force-stop Viber, then open Settings → Viber NoSlop. Restart Viber after changing options.");
        setContentView(info);
    }
}

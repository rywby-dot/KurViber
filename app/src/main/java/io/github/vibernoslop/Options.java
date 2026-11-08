package io.github.vibernoslop;

import android.content.Context;
import android.content.SharedPreferences;

final class Options {
    static final String DEFAULT_TITLE = "KurViber✅";
    enum Option {
        ADS_CHATS("Ads Chats", true),
        ADS_MORE("Ads More", true),
        ADS_OTHER("Ads Other", true),
        AD_REQUESTS("Ad Requests", true),
        ANALYTICS("Analytics", true),
        CRASHES("Crashes", true),
        VIBER_EVENTS("Viber Events", true),
        AI("Ai", true),
        TRANSLATE("Translate", true),
        FOLDERS("Folders", true),
        TAB_CHATS("Tab Chats", true),
        TAB_CALLS("Tab Calls", true),
        TAB_AI("Tab Ai", true),
        TAB_EXPLORE("Tab Explore", true),
        TAB_CONTACTS("Tab Contacts", true),
        TAB_MARKET("Tab Market", true),
        TAB_SHORTS("Tab Shorts", true),
        TAB_PAY("Tab Pay", true),
        TAB_MORE("Tab More", true),
        MORE_BUTTON("Show gear button for More", true),
        HIDE_FAB("Hide floating action buttons", true),
        SEARCH_FAB("Use search button for the current floating action", true),
        CAMERA("Hide toolbar camera", true),
        WHITE_HOME_ICONS("White icons on the home screen only", true),
        NAVBAR("Navbar", true);
        final String title;
        final boolean initial;
        Option(String title, boolean initial) { this.title = title; this.initial = initial; }
    }
    private static volatile SharedPreferences prefs;
    private static volatile boolean[] active;
    static void init(Context context) {
        prefs = context.getSharedPreferences("viber_noslop", Context.MODE_PRIVATE);
        boolean[] snapshot = new boolean[Option.values().length];
        for (Option option : Option.values()) snapshot[option.ordinal()] = saved(option);
        active = snapshot;
    }
    static boolean on(Option option) {
        boolean[] snapshot = active;
        return snapshot == null ? option.initial : snapshot[option.ordinal()];
    }
    static boolean saved(Option option) {
        SharedPreferences p = prefs;
        return p == null ? option.initial : p.getBoolean(option.name(), option.initial);
    }
    static void set(Option option, boolean value) { prefs.edit().putBoolean(option.name(), value).apply(); }
    static String text(String key, String fallback) { return prefs.getString(key, fallback); }
    static void setText(String key, String value) { prefs.edit().putString(key, value).apply(); }
    private Options() {}
}

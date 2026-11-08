package io.github.vibernoslop;
import static io.github.vibernoslop.Options.Option.*;
public final class UiRulesCheck {
    private static void check(boolean ok, String description) { if (!ok) throw new AssertionError(description); }
    public static void main(String[] args) {
        check(UiRules.layout("view_chat_list_ad_cell") == ADS_CHATS, "chat ad scope");
        check(UiRules.layout("view_chats_list_big_ad_cell") == ADS_CHATS, "big chat ad scope");
        check(UiRules.layout("view_more_screen_ad_cell") == ADS_MORE, "more ad scope");
        check(UiRules.layout("view_business_inbox_ad_cell") == ADS_OTHER, "other ad scope");
        check(UiRules.layout("msg_list_translate_button") == TRANSLATE, "translation button");
        check(UiRules.id("folder_tabs") == FOLDERS, "folder tabs");
        check(UiRules.id("bottom_nav_tab_messages") == TAB_CHATS, "chat tab option");
        for (Options.Option option : Options.Option.values()) check(Options.on(option), "enabled default: " + option);
        check(Options.DEFAULT_TITLE.equals("KurViber✅"), "appearance defaults");
        check(UiRules.id("bottom_nav_tab_gpt_chat") == AI, "AI tab disabled with integration");
        check(UiRules.layout("conversation_welcome_banner_layout") == null, "do not hide arbitrary banners");
        check(UiRules.id("adButton") == null, "do not hide an arbitrary inner view");
        check(UiRules.id("message") == null && UiRules.layout("fragment_more") == null, "ordinary content preserved");
        System.out.println("12 UI policy checks passed");
    }
}

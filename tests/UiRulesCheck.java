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
        check(UiRules.id("polish_messages_stub") == POLISH_MESSAGE, "polish composer root");
        check(UiRules.layout("polish_messages_menu") == POLISH_MESSAGE, "polish inflated stub layout");
        check(UiRules.id("options_menu_open_writing_assistant") == POLISH_MESSAGE, "writing assistant menu entry");
        check(UiRules.id("view_image_remix_cta") == IMAGE_REMIX, "gallery remix include ID override");
        check(UiRules.id("btn_image_remix_cta") == IMAGE_REMIX, "remix layout root");
        check(UiRules.layout("view_image_remix_cta_button") == IMAGE_REMIX, "gallery remix layout");
        check(UiRules.layout("msg_list_image_remix_button") == IMAGE_REMIX, "inline image remix button");
        check(UiRules.id("image_remix_fab_container") == IMAGE_REMIX, "remix floating action container");
        check(UiRules.id("imageRemixProgress") == null, "do not target inner progress controls");
        check(UiRules.layout("fullscreen_gallery_view") == null, "keep actual image viewer");
        System.out.println("UI policy checks passed");
    }
}

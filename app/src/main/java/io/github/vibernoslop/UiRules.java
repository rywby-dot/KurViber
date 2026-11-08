package io.github.vibernoslop;

import static io.github.vibernoslop.Options.Option.*;
import io.github.vibernoslop.Options.Option;

/** Resource entry names, never version-specific integer IDs or translated labels. */
final class UiRules {
    static Option id(String name) {
        switch (name) {
            case "fab_container": return HIDE_FAB;
            case "folder_tabs": return FOLDERS;
            case "translateButtonView": return TRANSLATE;
            case "bottom_nav_bar": return NAVBAR;
            case "bottom_nav_tab_messages": return TAB_CHATS;
            case "bottom_nav_tab_calls": return TAB_CALLS;
            case "bottom_nav_tab_ai_chat": case "bottom_nav_tab_gpt_chat":
                return Options.on(AI) ? AI : TAB_AI;
            case "bottom_nav_tab_contacts": return TAB_CONTACTS;
            case "bottom_nav_tab_explore": return TAB_EXPLORE;
            case "bottom_nav_tab_marketplace": return TAB_MARKET;
            case "bottom_nav_tab_shortica": return TAB_SHORTS;
            case "bottom_nav_tab_v_pay": return TAB_PAY;
            case "bottom_nav_tab_more": return TAB_MORE;
            case "ask_chatgpt_floating_button":
            case "chat_info_option_connect_chatgpt_account":
            case "menu_ask_chatgpt": case "menu_ask_chatgpt_in_chat":
            case "menu_ask_chatgpt_privately": return AI;
            case "moreScreenAd": return ADS_MORE;
            case "search_ad_slot": case "gif_ad_container": return ADS_OTHER;
            default: return null;
        }
    }
    static Option layout(String name) {
        switch (name) {
            case "view_chat_list_ad_cell":
            case "view_chats_list_big_ad_cell":
            case "view_chats_list_big_font_big_ad_cell":
            case "view_chats_list_ad_placeholder":
            case "view_chats_list_big_ad_placeholder":
            case "view_chats_list_big_font_big_ad_placeholder":
            case "view_chats_list_ad_google_unified":
            case "view_chats_list_big_ad_google_unified":
            case "view_chats_list_big_font_big_ad_google_unified": return ADS_CHATS;
            case "view_more_screen_ad_cell":
            case "view_more_screen_ad":
            case "view_more_screen_ad_placeholder":
            case "view_more_screen_ad_google_unified": return ADS_MORE;
            case "view_business_inbox_ad_cell":
            case "view_calls_tab_list_ad": case "calls_tab_contacts_ad_cell":
            case "view_chat_ext_ad_cell": case "view_chats_ext_ad_placeholder":
            case "view_search_ad_slot": case "view_search_ad_placeholder":
            case "view_scoreboard_ad_placeholder": case "view_explore_ad_new":
            case "view_explore_ad_google_unified": case "view_explore_ad_google_unified_new":
            case "post_call_banner_ad_placeholder": case "post_call_native_ad_placeholder":
            case "post_call_native_ad_google_unified_placeholder":
            case "post_call_promotion_ad_placeholder": return ADS_OTHER;
            case "msg_list_translate_button": return TRANSLATE;
            case "search_chatgpt_carousel_item": return AI;
            default: return null;
        }
    }
    private UiRules() {}
}

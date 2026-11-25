package io.github.vibernoslop;

public final class AiFeatureRulesCheck {
    private static void check(boolean condition, String label) {
        if (!condition) throw new AssertionError(label);
    }
    public static void main(String[] args) {
        check(AiFeatureRules.experiment("core_ai_writingassistant") == Options.Option.POLISH_MESSAGE,
            "Polish is independent of ChatGPT");
        check(AiFeatureRules.experiment("core_chats_imageremix") == Options.Option.IMAGE_REMIX,
            "Image Remix has its own switch and non-null default");
        for (String key : new String[]{"core_ai_chatgptintegration", "core_ai_chatgpttab", "core_ai_chatgpttabv2"})
            check(AiFeatureRules.experiment(key) == Options.Option.AI, "Existing ChatGPT key: " + key);
        for (String key : new String[]{"core_chats_imageremix_bdayflow", "core_ai_writingassistant_v2",
                "core_oai_imageremix_login_cta", "core_ai_chatsummary_anonymization", "", null})
            check(AiFeatureRules.experiment(key) == null, "Do not change unrelated/future models: " + key);
        check(Options.on(Options.Option.POLISH_MESSAGE) && Options.on(Options.Option.IMAGE_REMIX),
            "Both new controls default to enabled");
        System.out.println("Independent AI feature policy checks passed");
    }
}

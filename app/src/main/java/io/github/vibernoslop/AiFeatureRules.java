package io.github.vibernoslop;

/** Exact experiment keys: never apply nullable results to unrelated feature models. */
final class AiFeatureRules {
    static Options.Option experiment(String key) {
        if (key == null) return null;
        switch (key) {
            case "core_ai_chatgptintegration":
            case "core_ai_chatgpttab":
            case "core_ai_chatgpttabv2": return Options.Option.AI;
            case "core_ai_writingassistant": return Options.Option.POLISH_MESSAGE;
            case "core_chats_imageremix": return Options.Option.IMAGE_REMIX;
            default: return null;
        }
    }
    private AiFeatureRules() {}
}

package io.github.vibernoslop;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;

final class WritingAssistantHooks {
    static void install(ClassLoader loader) throws Exception {
        Class<?> row = Class.forName("com.viber.voip.messages.ui.chatactions.ChatActionPillsRowView", false, loader);
        Method target = null, publicSetter = null;
        int publicSetters = 0;
        for (Method method : row.getDeclaredMethods()) {
            if (Modifier.isStatic(method.getModifiers()) || method.getReturnType() != void.class
                || method.getParameterCount() != 1 || method.getParameterTypes()[0] != List.class) continue;
            if (method.getName().equals("setContent")) { target = method; break; }
            if (Modifier.isPublic(method.getModifiers())) { publicSetter = method; publicSetters++; }
        }
        if (target == null && publicSetters == 1) target = publicSetter;
        if (target == null) throw new NoSuchMethodException("No unique ChatActionPillsRowView public void(List) setter");
        XposedBridge.hookMethod(target, new XC_MethodHook() {
            @Override protected void beforeHookedMethod(MethodHookParam p) {
                if (!Options.on(Options.Option.POLISH_MESSAGE) || !(p.args[0] instanceof List)) return;
                WritingAssistantPillFilter.Result<?> result = WritingAssistantPillFilter.filter((List<?>) p.args[0], true);
                if (result.removed != 0) {
                    p.args[0] = result.providers;
                    HookLog.add("Polish: Writing Assistant pill provider removed; count=" + result.removed);
                }
                if (result.unresolved != 0)
                    HookLog.add("Polish: unrecognized pill providers preserved; count=" + result.unresolved);
            }
        });
        HookLog.add("Polish: shared action-pill filter installed / " + target.toGenericString());
    }
    private WritingAssistantHooks() {}
}

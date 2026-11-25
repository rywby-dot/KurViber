package io.github.vibernoslop;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Filter feature providers, not rendered text or the entire shared suggestion row. */
final class WritingAssistantPillFilter {
    private static final Map<Class<?>, Optional<Method>> readers = new ConcurrentHashMap<>();
    static final class Result<T> {
        final List<T> providers;
        final int removed, unresolved;
        Result(List<T> providers, int removed, int unresolved) {
            this.providers = providers; this.removed = removed; this.unresolved = unresolved;
        }
    }
    static <T> Result<T> filter(List<T> providers, boolean enabled) {
        if (!enabled) return new Result<>(providers, 0, 0);
        List<T> kept = new ArrayList<>(providers.size());
        int removed = 0, unresolved = 0;
        for (T provider : providers) {
            boolean assistant = false;
            if (provider != null) {
                try {
                    Optional<Method> reader = readers.computeIfAbsent(provider.getClass(), WritingAssistantPillFilter::keyReader);
                    if (reader.isPresent()) assistant = "ai_writing_assistant".equals(reader.get().invoke(provider));
                    else unresolved++;
                } catch (ReflectiveOperationException | RuntimeException error) { unresolved++; }
            }
            if (assistant) removed++; else kept.add(provider);
        }
        return new Result<>(removed == 0 ? providers : kept, removed, unresolved);
    }
    private static Optional<Method> keyReader(Class<?> type) {
        Map<String, Method> methods = new LinkedHashMap<>();
        for (Class<?> current = type; current != null; current = current.getSuperclass())
            for (Class<?> contract : current.getInterfaces())
                for (Method method : contract.getMethods())
                    if (!Modifier.isStatic(method.getModifiers()) && method.getParameterCount() == 0
                        && method.getReturnType() == String.class) methods.put(method.getName(), method);
        if (methods.size() != 1) return Optional.empty();
        Method reader = methods.values().iterator().next();
        reader.setAccessible(true);
        return Optional.of(reader);
    }
    private WritingAssistantPillFilter() {}
}

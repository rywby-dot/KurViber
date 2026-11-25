package io.github.vibernoslop;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class WritingAssistantPillFilterCheck {
    interface OldContract { String b(); Object getState(); }
    interface RenamedContract { String x(); }
    interface AmbiguousContract { String x(); String y(); }
    static final class OldAssistant implements OldContract {
        public String b() { return "ai_writing_assistant"; }
        public Object getState() { throw new AssertionError("Do not read state"); }
        public String toString() { throw new AssertionError("Do not inspect text"); }
    }
    static final class RenamedAssistant implements RenamedContract {
        public String x() { return "ai_writing_assistant"; }
    }
    static final class BookLink implements OldContract {
        public String b() { return "book_link"; }
        public Object getState() { throw new AssertionError("Do not read state"); }
    }
    static final class Ambiguous implements AmbiguousContract {
        public String x() { throw new AssertionError("Must not guess a getter"); }
        public String y() { throw new AssertionError("Must not guess a getter"); }
    }
    static final class Broken implements RenamedContract {
        public String x() { throw new IllegalStateException("Changed provider"); }
    }
    private static void check(boolean ok, String label) { if (!ok) throw new AssertionError(label); }
    public static void main(String[] args) {
        Object book = new BookLink(), unknown = new Object(), assistant = new OldAssistant();
        List<Object> mixed = Collections.unmodifiableList(Arrays.asList(book, assistant, unknown, new RenamedAssistant()));
        WritingAssistantPillFilter.Result<Object> result = WritingAssistantPillFilter.filter(mixed, true);
        check(result.removed == 2, "Remove old and renamed providers by stable feature key");
        check(result.providers.size() == 2 && result.providers.get(0) == book && result.providers.get(1) == unknown,
            "Keep unrelated suggestions and their order");
        check(mixed.size() == 4, "Do not mutate immutable controller input");
        check(WritingAssistantPillFilter.filter(mixed, false).providers == mixed, "Switch off preserves exact input");
        check(WritingAssistantPillFilter.filter(Collections.singletonList(assistant), true).providers.isEmpty(),
            "Assistant-only row becomes empty");
        List<Object> future = Arrays.asList(new Ambiguous(), new Broken());
        check(WritingAssistantPillFilter.filter(future, true).providers == future,
            "Ambiguous and throwing providers are kept");
        check(WritingAssistantPillFilter.filter(Collections.emptyList(), true).removed == 0, "Empty row is safe");
        System.out.println("Writing Assistant Compose pill filtering checks passed");
    }
}

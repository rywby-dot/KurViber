package io.github.vibernoslop;

public final class DisabledExperimentDefaultCheck {
    static final class Disabled {
        static final Disabled INSTANCE = new Disabled();
        @Override public String toString() { return "Disabled"; }
    }
    static final class OtherDisabled {
        static final OtherDisabled INSTANCE = new OtherDisabled();
        @Override public String toString() { return "Disabled"; }
    }
    static final class Stateful {
        static final Stateful INSTANCE = new Stateful();
        int data;
        @Override public String toString() { return "Disabled"; }
    }
    static final class Wrapper {
        final int syntheticCase = 1;
        final Object defaultValue;
        final Object extra;
        Wrapper(Object defaultValue, Object extra) { this.defaultValue = defaultValue; this.extra = extra; }
    }
    public static void main(String[] args) throws Exception {
        if (DisabledExperimentDefault.find(new Wrapper(Disabled.INSTANCE, new Object())) != Disabled.INSTANCE)
            throw new AssertionError("Use captured singleton rather than null or a new instance");
        if (DisabledExperimentDefault.find(new Wrapper(Stateful.INSTANCE, null)) != null)
            throw new AssertionError("Reject stateful default");
        if (DisabledExperimentDefault.find(new Wrapper(null, new Object())) != null)
            throw new AssertionError("Reject unmatched future factory shape");
        try {
            DisabledExperimentDefault.find(new Wrapper(Disabled.INSTANCE, OtherDisabled.INSTANCE));
            throw new AssertionError("Ambiguous defaults must not pick an arbitrary result");
        } catch (NoSuchFieldException expected) { }
        System.out.println("Image Remix disabled-default validation checks passed");
    }
}

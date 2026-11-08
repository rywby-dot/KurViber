package io.github.vibernoslop;
import java.util.Set;
public class DexFinderCheck {
    public static void main(String[] args) throws Exception {
        long start = System.nanoTime();
        Set<String> matches = DexFinder.find(args[0], DexFinder.EXPERIMENT_ANCHOR);
        if (matches.size() != 1) throw new AssertionError("Expected unique mapper, found " + matches);
        java.util.Map<String, Set<String>> sdk = DexFinder.findAll(args[0], new String[]{
            "Setting Braze SDK disabled to: ", "Crashlytics automatic data collection ", "Failed to load ad."});
        for (java.util.Map.Entry<String, Set<String>> entry : sdk.entrySet()) {
            if (entry.getValue().isEmpty()) throw new AssertionError("Missing SDK anchor: " + entry.getKey());
            System.out.println(entry.getKey() + " -> " + entry.getValue());
        }
        System.out.println("Mapper: " + matches + "; " + (System.nanoTime() - start) / 1000000 + " ms");
    }
}

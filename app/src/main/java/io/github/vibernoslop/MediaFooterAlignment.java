package io.github.vibernoslop;

import android.text.Layout;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import de.robv.android.xposed.XposedHelpers;

/** Align the photo sender's single text line with the currently visible reaction button. */
final class MediaFooterAlignment {
    private static final Map<View, Boolean> installed = Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<View, Integer> originalMargins = new WeakHashMap<>();
    static void observe(View view) {
        if (!Options.on(Options.Option.IMAGE_REMIX) || !(view instanceof ViewGroup)
            || !"mediaDetailsContainer".equals(UiHooks.name(view)) || installed.containsKey(view)) return;
        installed.put(view, true);
        view.getViewTreeObserver().addOnGlobalLayoutListener(() -> align(view));
        view.post(() -> align(view));
    }
    private static View find(View root, String name) {
        int id = root.getResources().getIdentifier(name, "id", "com.viber.voip");
        return id == 0 ? null : root.findViewById(id);
    }
    private static TextView textChild(View view) {
        if (view instanceof TextView) return (TextView) view;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                TextView found = textChild(group.getChildAt(i));
                if (found != null) return found;
            }
        }
        return null;
    }
    private static boolean visible(View view) {
        return view != null && view.getVisibility() == View.VISIBLE && view.isShown() && view.getHeight() > 0;
    }
    private static void align(View root) {
        if (!Options.on(Options.Option.IMAGE_REMIX) || !root.isAttachedToWindow()) return;
        try {
            // The same description widget is used elsewhere; restrict this to the photo
            // details layout containing the Remix button and its adjacent reaction control.
            if (find(root, "imageRemixCtaButton") == null) return;
            View description = find(root, "descriptionView");
            if (description == null || !description.getClass().getName()
                .equals("com.viber.expandabletextview.ExpandableTextView") || !visible(description)) return;
            View reaction = find(root, "reactionViewExtended");
            if (!visible(reaction)) reaction = find(root, "reactionView");
            if (!visible(reaction)) return;
            TextView text = textChild(description);
            if (!visible(text) || text.getScaleY() != 1f || description.getScaleY() != 1f) return;
            Layout layout = text.getLayout();
            if (layout == null) return;
            if (layout.getLineCount() != 1) {
                // Media pages can reuse the same widget for a photo with a caption.
                Integer original = originalMargins.remove(description);
                if (original != null) {
                    XposedHelpers.callMethod(description, "setTextMarginBottom", original);
                    description.requestLayout();
                }
                return;
            }
            int[] descriptionPosition = new int[2], reactionPosition = new int[2];
            description.getLocationInWindow(descriptionPosition);
            reaction.getLocationInWindow(reactionPosition);
            float lineCenter = text.getTotalPaddingTop() - text.getScrollY()
                + (layout.getLineTop(0) + layout.getLineBottom(0)) / 2f;
            Integer margin = MediaFooterGeometry.alignedBottomMargin(
                descriptionPosition[1] + description.getHeight(), text.getHeight(), lineCenter,
                reactionPosition[1], reaction.getHeight(), layout.getLineCount());
            if (margin == null) return;
            int previous = (Integer) XposedHelpers.callMethod(description, "getTextMarginBottom");
            if (previous == margin) return;
            if (!originalMargins.containsKey(description)) originalMargins.put(description, previous);
            XposedHelpers.callMethod(description, "setTextMarginBottom", margin);
            // The widget's setter updates the child's margin but does not always request
            // another layout when called after a completed layout pass.
            description.requestLayout();
            HookLog.add("Media footer: sender line aligned to reaction center");
        } catch (Throwable error) {
            HookLog.add("Media footer alignment skipped: " + error.getClass().getSimpleName() + ": " + error.getMessage());
        }
    }
    private MediaFooterAlignment() {}
}

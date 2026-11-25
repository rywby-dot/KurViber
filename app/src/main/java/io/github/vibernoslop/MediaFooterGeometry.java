package io.github.vibernoslop;

/** Derive the footer margin from measured text and reaction bounds in one coordinate space. */
final class MediaFooterGeometry {
    static Integer alignedBottomMargin(float descriptionBottom, int textHeight, float lineCenter,
                                       float reactionTop, int reactionHeight, int lineCount) {
        if (textHeight <= 0 || reactionHeight <= 0 || lineCount != 1
            || !Float.isFinite(descriptionBottom) || !Float.isFinite(lineCenter)
            || !Float.isFinite(reactionTop)) return null;
        // ExpandableTextView positions its child at bottom - height - textMarginBottom.
        float targetCenter = reactionTop + reactionHeight / 2f;
        float margin = descriptionBottom - textHeight + lineCenter - targetCenter;
        if (margin < 0 || margin > Integer.MAX_VALUE) return null;
        return Math.round(margin);
    }
    private MediaFooterGeometry() {}
}

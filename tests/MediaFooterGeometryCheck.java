package io.github.vibernoslop;

public final class MediaFooterGeometryCheck {
    private static void aligned(float bottom, int textHeight, float lineCenter, float reactionTop, int reactionHeight) {
        Integer margin = MediaFooterGeometry.alignedBottomMargin(bottom, textHeight, lineCenter, reactionTop, reactionHeight, 1);
        if (margin == null) throw new AssertionError("Expected feasible footer alignment");
        float textCenter = bottom - textHeight - margin + lineCenter;
        float reactionCenter = reactionTop + reactionHeight / 2f;
        if (Math.abs(textCenter - reactionCenter) > .5001f)
            throw new AssertionError("Text and reaction centers differ: " + textCenter + " / " + reactionCenter);
    }
    public static void main(String[] args) {
        aligned(2600, 48, 24, 2480, 108);
        aligned(2600, 72, 34, 2480, 108); // Larger font.
        aligned(2600, 48, 24, 2460, 132); // Selected reaction has different geometry.
        aligned(1600, 27, 13.5f, 1520, 55); // Half-pixel line center and different density.
        aligned(2750, 48, 24, 2630, 108); // Both controls translated in the window.
        if (MediaFooterGeometry.alignedBottomMargin(2600, 48, 24, 2480, 0, 1) != null)
            throw new AssertionError("Wait for reaction measurement");
        if (MediaFooterGeometry.alignedBottomMargin(2600, 160, 24, 2480, 108, 4) != null)
            throw new AssertionError("Do not push a multiline caption beyond the viewport");
        if (MediaFooterGeometry.alignedBottomMargin(200, 48, 24, 300, 108, 1) != null)
            throw new AssertionError("Do not use negative margins for unrelated/out-of-bounds controls");
        System.out.println("Measured sender/reaction alignment checks passed");
    }
}

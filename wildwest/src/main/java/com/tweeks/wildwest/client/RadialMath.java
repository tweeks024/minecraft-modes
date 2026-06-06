package com.tweeks.wildwest.client;

/**
 * Pure math for the radial stone picker. Tested in isolation so we
 * don't need a render context to verify wedge selection.
 *
 * <p>Convention: wedge 0 is straight up (12 o'clock), wedges proceed
 * clockwise. Six wedges of 60° each.
 */
public final class RadialMath {
    private RadialMath() {}

    public static int wedgeFromMouse(double mouseX, double mouseY,
                                     double centerX, double centerY,
                                     double deadzoneRadiusPx) {
        double dx = mouseX - centerX;
        double dy = mouseY - centerY;
        double distSq = dx * dx + dy * dy;
        if (distSq < deadzoneRadiusPx * deadzoneRadiusPx) return -1;

        double angle = Math.atan2(dx, -dy);
        if (angle < 0) angle += Math.PI * 2;

        double wedgeRad = Math.PI * 2 / 6;
        double shifted = angle + wedgeRad / 2;
        if (shifted >= Math.PI * 2) shifted -= Math.PI * 2;

        return (int) (shifted / wedgeRad);
    }
}

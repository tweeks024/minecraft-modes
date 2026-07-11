package com.tweeks.starwars.client;

/**
 * Pure math for the Force power radial picker. Tested in isolation so we
 * don't need a render context to verify wedge selection.
 *
 * <p>Convention: wedge 0 is straight up (12 o'clock), wedges proceed
 * clockwise. Generalized port of wildwest's {@code RadialMath} with a
 * {@code wedgeCount} parameter instead of a hardcoded six.
 */
public final class SwRadialMath {
    private SwRadialMath() {}

    public static int wedgeFromMouse(double mouseX, double mouseY,
                                     double centerX, double centerY,
                                     double deadzoneRadiusPx, int wedgeCount) {
        double dx = mouseX - centerX;
        double dy = mouseY - centerY;
        double distSq = dx * dx + dy * dy;
        if (distSq < deadzoneRadiusPx * deadzoneRadiusPx) return -1;

        double angle = Math.atan2(dx, -dy);
        if (angle < 0) angle += Math.PI * 2;

        double wedgeRad = Math.PI * 2 / wedgeCount;
        double shifted = angle + wedgeRad / 2;
        if (shifted >= Math.PI * 2) shifted -= Math.PI * 2;

        int wedge = (int) (shifted / wedgeRad);
        // Floating-point edge case: an angle of exactly 2π after shift+subtract
        // can still divide to wedgeCount on some JVMs. Clamp so ForcePower.byIndex
        // doesn't quietly fall back to PUSH.
        return wedge >= wedgeCount ? 0 : wedge;
    }
}

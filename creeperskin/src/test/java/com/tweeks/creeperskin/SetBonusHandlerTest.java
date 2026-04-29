package com.tweeks.creeperskin;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit-test for {@link SetBonusHandler}.
 *
 * <p>Best-effort: mocking {@link net.minecraft.world.entity.LivingEntity} is
 * difficult because most of its behavior depends on a live {@code Level} and
 * registry holders. We keep one self-contained smoke test here that proves
 * the helper compiles and the file structure is right; full behavioral
 * verification of {@link SetBonusHandler#isWearingFullSet} happens in the
 * manual smoke test (Task 9). If a future test framework supports
 * lightweight LivingEntity mocks (e.g. SpongeMixin test harness), expand
 * this class to drive {@code isWearingFullSet} with mocks.
 */
class SetBonusHandlerTest {

    @Test
    void classLoadsAndExposesIsWearingFullSet() throws Exception {
        // Reflection only — calling isWearingFullSet with null would NPE
        // since the helper dereferences entity.getItemBySlot(...). The
        // method-existence check guards against the helper being deleted
        // or renamed in a refactor.
        var method = SetBonusHandler.class.getMethod(
            "isWearingFullSet",
            net.minecraft.world.entity.LivingEntity.class);
        assertTrue(java.lang.reflect.Modifier.isStatic(method.getModifiers()),
            "isWearingFullSet should be static");
        assertTrue(java.lang.reflect.Modifier.isPublic(method.getModifiers()),
            "isWearingFullSet should be public");
    }
}

package com.tweeks.wildwest.effect;

import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;

public final class HandSnapshot {
    private HandSnapshot() {}

    public static final String KEY_MAIN = "wildwest:pre_zombified_mainhand";
    public static final String KEY_OFF  = "wildwest:pre_zombified_offhand";

    /** Snapshot held items into persistent data and clear the slots. No-op if already snapshotted. */
    public static void snapshotAndClear(Mob mob) {
        var pd = mob.getPersistentData();
        if (pd.contains(KEY_MAIN) || pd.contains(KEY_OFF)) return;

        ItemStack main = mob.getItemBySlot(EquipmentSlot.MAINHAND);
        ItemStack off  = mob.getItemBySlot(EquipmentSlot.OFFHAND);

        RegistryOps<Tag> ops = RegistryOps.create(NbtOps.INSTANCE, mob.registryAccess());

        if (!main.isEmpty()) {
            ItemStack.CODEC.encodeStart(ops, main).ifSuccess(tag -> pd.put(KEY_MAIN, tag));
        }
        if (!off.isEmpty()) {
            ItemStack.CODEC.encodeStart(ops, off).ifSuccess(tag -> pd.put(KEY_OFF, tag));
        }

        mob.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        mob.setItemSlot(EquipmentSlot.OFFHAND,  ItemStack.EMPTY);
    }

    /** Restore previously snapshotted held items. Clears the snapshot keys after restoring. */
    public static void restore(Mob mob) {
        var pd = mob.getPersistentData();

        RegistryOps<Tag> ops = RegistryOps.create(NbtOps.INSTANCE, mob.registryAccess());

        if (pd.contains(KEY_MAIN)) {
            Tag tag = pd.get(KEY_MAIN);
            if (tag != null) {
                ItemStack.CODEC.parse(ops, tag).ifSuccess(s -> mob.setItemSlot(EquipmentSlot.MAINHAND, s));
            }
            pd.remove(KEY_MAIN);
        }
        if (pd.contains(KEY_OFF)) {
            Tag tag = pd.get(KEY_OFF);
            if (tag != null) {
                ItemStack.CODEC.parse(ops, tag).ifSuccess(s -> mob.setItemSlot(EquipmentSlot.OFFHAND, s));
            }
            pd.remove(KEY_OFF);
        }
    }
}

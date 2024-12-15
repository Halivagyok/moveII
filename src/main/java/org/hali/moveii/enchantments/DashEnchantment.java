package org.hali.moveii.enchantments;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentTarget;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;

public class DashEnchantment extends Enchantment {
    public DashEnchantment() {
        super(Rarity.RARE, EnchantmentTarget.BREAKABLE, new EquipmentSlot[]{EquipmentSlot.MAINHAND});
    }

    @Override
    public boolean isAcceptableItem(ItemStack stack) {
        // Check for WEAPON, DIGGER, and TRIDENT
        return EnchantmentTarget.WEAPON.isAcceptableItem(stack.getItem())
                || EnchantmentTarget.DIGGER.isAcceptableItem(stack.getItem())
                || EnchantmentTarget.TRIDENT.isAcceptableItem(stack.getItem());
    }

    @Override
    public int getMaxLevel() {
        return 2;
    }
}

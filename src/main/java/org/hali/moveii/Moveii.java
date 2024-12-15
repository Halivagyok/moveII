package org.hali.moveii;

import net.fabricmc.api.ModInitializer;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import org.hali.moveii.enchantments.DashEnchantment;
import org.hali.moveii.enchantments.DoubleJumpEnchantment;
import org.hali.moveii.enchantments.FastSwingEnchantment;
import org.hali.moveii.enchantments.ReachEnchantment;

public class Moveii implements ModInitializer {
    public static final String MOD_ID = "moveii";

    // Enchantment instances
    public static final Enchantment DASH = new DashEnchantment();
    public static final Enchantment DOUBLE_JUMP = new DoubleJumpEnchantment();
    public static final Enchantment REACH = new ReachEnchantment();
    public static final Enchantment FAST_SWING = new FastSwingEnchantment();

    public static int getEnchantmentLevel(ItemStack stack, Enchantment enchantment) {
        return EnchantmentHelper.getLevel(enchantment, stack);
    }


    @Override
    public void onInitialize() {
        // Register enchantments
        registerEnchantments();
    }

    private void registerEnchantments() {
        Registry.register(Registries.ENCHANTMENT, new Identifier(MOD_ID, "dash"), DASH);
        Registry.register(Registries.ENCHANTMENT, new Identifier(MOD_ID, "double_jump"), DOUBLE_JUMP);
        Registry.register(Registries.ENCHANTMENT, new Identifier(MOD_ID, "reach"), REACH);
        Registry.register(Registries.ENCHANTMENT, new Identifier(MOD_ID, "fast_swing"), FAST_SWING);
    }
}
package org.hali.moveii.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Vec3d;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import org.hali.moveii.Moveii;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MoveiiClient implements ClientModInitializer {
    private static final MinecraftClient client = MinecraftClient.getInstance();
    private boolean hasDoubleJumped = false;
    private final Map<PlayerEntity, Long> lastDashTime = new HashMap<>();

    private static final UUID REACH_BOOST_UUID = UUID.fromString("e5b8f18c-d6b8-4d74-b8a5-4fb644df76ab");
    private static final UUID SWING_SPEED_UUID = UUID.fromString("72832b85-47a7-4b39-9081-1ecf38d8bda1");

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player != null) {
                handleDoubleJump(client.player);
                handleDash(client.player);
                applyReachBoost(client.player);
                applyFastSwing(client.player);
            }
        });
    }

    private void handleDoubleJump(PlayerEntity player) {
        ItemStack boots = player.getInventory().getArmorStack(0);
        if (boots == null || !boots.hasEnchantments()) return;

        int doubleJumpLevel = Moveii.getEnchantmentLevel(boots, Moveii.DOUBLE_JUMP);
        if (doubleJumpLevel == 0) return;

        double jumpHeight = 0.4 + (0.1 * doubleJumpLevel);
        if (!player.isOnGround() && !player.isSwimming()) {
            if (!hasDoubleJumped && client.options.jumpKey.isPressed()) {
                if (player.getVelocity().y < 0) {
                    Vec3d velocity = player.getVelocity();
                    player.setVelocity(velocity.x, jumpHeight, velocity.z);
                    hasDoubleJumped = true;
                    player.fallDistance = 0.0f;
                }
            }
        } else if (player.isOnGround()) {
            hasDoubleJumped = false;
        }
    }

    private void handleDash(PlayerEntity player) {
        ItemStack mainHand = player.getMainHandStack();
        if (mainHand == null || !mainHand.hasEnchantments()) return;

        int dashLevel = Moveii.getEnchantmentLevel(mainHand, Moveii.DASH);
        if (dashLevel == 0) return;

        double dashLength = 1.0 + (0.1 * dashLevel);
        long dashCooldown = (dashLevel == 1) ? 700 : 900;

        if (client.options.useKey.isPressed()) {
            long currentTime = System.currentTimeMillis();
            if (lastDashTime.getOrDefault(player, 0L) + dashCooldown > currentTime) {
                return;
            }

            Vec3d lookVector = player.getRotationVec(1.0F).normalize();
            Vec3d dashVelocity = lookVector.multiply(dashLength);
            player.setVelocity(dashVelocity.x, player.getVelocity().y, dashVelocity.z);

            lastDashTime.put(player, currentTime);
        }
    }

    private void applyReachBoost(PlayerEntity player) {
        ItemStack mainHand = player.getMainHandStack();
        if (mainHand == null || !mainHand.hasEnchantments()) return;

        int reachLevel = Moveii.getEnchantmentLevel(mainHand, Moveii.REACH);
        if (reachLevel > 0) {
            double additionalReach = 1.0 + (0.5 * reachLevel);

            var reachAttribute = player.getAttributes().getCustomInstance(EntityAttributes.GENERIC_ATTACK_SPEED);
            if (reachAttribute != null) {
                reachAttribute.removeModifier(REACH_BOOST_UUID);
                reachAttribute.addPersistentModifier(new EntityAttributeModifier(REACH_BOOST_UUID, "Reach boost", additionalReach, EntityAttributeModifier.Operation.ADDITION));
            }
        }
    }

    private void applyFastSwing(PlayerEntity player) {
        ItemStack mainHand = player.getMainHandStack();
        if (mainHand == null || !mainHand.hasEnchantments()) return;

        int swingLevel = Moveii.getEnchantmentLevel(mainHand, Moveii.FAST_SWING);
        if (swingLevel > 0) {
            double swingSpeedBoost = 1.0 + (0.1 * swingLevel);

            var attackSpeedAttribute = player.getAttributes().getCustomInstance(EntityAttributes.GENERIC_ATTACK_SPEED);
            if (attackSpeedAttribute != null) {
                attackSpeedAttribute.removeModifier(SWING_SPEED_UUID);
                attackSpeedAttribute.addPersistentModifier(new EntityAttributeModifier(SWING_SPEED_UUID, "Fast Swing", swingSpeedBoost, EntityAttributeModifier.Operation.MULTIPLY_TOTAL));
            }
        }
    }
}

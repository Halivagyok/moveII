package org.hali.moveii.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

import org.hali.moveii.Moveii;

import java.util.HashMap;
import java.util.Map;

public class MoveiiClient implements ClientModInitializer {
    private static final MinecraftClient client = MinecraftClient.getInstance();
    private boolean hasDoubleJumped = false;
    private final Map<PlayerEntity, Long> lastDashTime = new HashMap<>();

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player != null) {
                handleDoubleJump(client.player);
                handleDash(client.player);
            }
        });
    }

    private void handleDoubleJump(PlayerEntity player) {
        if (!Moveii.getDoubleJumpEnabled().getOrDefault(player, false)) return;

        if (!player.isOnGround() && !player.isSwimming()) {
            if (!hasDoubleJumped && client.options.jumpKey.isPressed()) {
                if (player.getVelocity().y < 0) {
                    double jumpHeight = Moveii.getDoubleJumpHeight().getOrDefault(player, 0.8);
                    Vec3d velocity = player.getVelocity();
                    player.setVelocity(velocity.x, jumpHeight, velocity.z);
                    hasDoubleJumped = true;
                    player.fallDistance = 0.0f; // Prevent fall damage
                }
            }
        } else if (player.isOnGround()) {
            hasDoubleJumped = false;
        }
    }

    private void handleDash(PlayerEntity player) {
        if (!Moveii.getDashEnabled().getOrDefault(player, false)) return;

        if (client.options.sneakKey.isPressed()) {
            long currentTime = System.currentTimeMillis();
            long dashCooldown = Moveii.getDashCooldown().getOrDefault(player, 1000L);

            if (lastDashTime.getOrDefault(player, 0L) + dashCooldown > currentTime) {
                return;
            }

            double dashLength = Moveii.getDashLength().getOrDefault(player, 1.5);
            Vec3d lookVector = player.getRotationVec(1.0F).normalize();
            Vec3d dashVelocity = lookVector.multiply(dashLength);
            player.setVelocity(dashVelocity.x, player.getVelocity().y, dashVelocity.z);

            lastDashTime.put(player, currentTime);
        }
    }
}

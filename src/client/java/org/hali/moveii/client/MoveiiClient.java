package org.hali.moveii.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;

public class MoveiiClient implements ClientModInitializer {

    private boolean hasDoubleJumped = false;

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            PlayerEntity player = client.player;

            if (player != null) {
                // Check if the player is jumping and hasn't already double-jumped
                if (client.options.jumpKey.isPressed() && !hasDoubleJumped) {
                    // Do your double jump logic here
                    handleDoubleJump(player);
                }

                // Handle dashing only when the "Special Stick" is in the main or off-hand
                if (isHoldingSpecialStick(player)) {
                    handleDash(player);
                }
            }
        });
    }

    private void handleDoubleJump(PlayerEntity player) {
        // Example double jump behavior, modify as needed
        if (!hasDoubleJumped) {
            player.addVelocity(0, 0.5, 0); // Adjust this for double jump height
            hasDoubleJumped = true; // Set the flag so it doesn't double jump again
        }
    }

    private boolean isHoldingSpecialStick(PlayerEntity player) {
        // Check if the player is holding the "Special Stick" in either hand
        return (player.getMainHandStack().getItem() == Items.STICK && player.getMainHandStack().hasNbt()
                && player.getMainHandStack().getNbt().contains("AllowedItem")
                && "Special".equals(player.getMainHandStack().getNbt().getString("AllowedItem")))
                || (player.getOffHandStack().getItem() == Items.STICK && player.getOffHandStack().hasNbt()
                && player.getOffHandStack().getNbt().contains("AllowedItem")
                && "Special".equals(player.getOffHandStack().getNbt().getString("AllowedItem")));
    }

    private void handleDash(PlayerEntity player) {
        if (MinecraftClient.getInstance().options.sneakKey.isPressed()) {
            // Dash effect, adjust the values to make it feel right for your game
            player.addVelocity(player.getRotationVector().x * 1.5, 0, player.getRotationVector().z * 1.5);
        }
    }
}

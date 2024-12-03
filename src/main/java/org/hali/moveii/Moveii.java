package org.hali.moveii;

import net.fabricmc.api.ModInitializer;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;





import java.util.HashMap;
import java.util.Map;

public class Moveii implements ModInitializer, ClientModInitializer {
    private static final String MOD_ID = "moveII";

    private final Map<ServerPlayerEntity, Boolean> dashEnabled = new HashMap<>();
    private final Map<ServerPlayerEntity, Boolean> doubleJumpEnabled = new HashMap<>();
    private static final String REQUIRED_NBT_KEY = "AllowedItem";
    private static final String REQUIRED_NBT_VALUE = "Special";

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            registerGiveItemCommand(dispatcher);
        });
        // Register admin commands
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            registerToggleCommand(dispatcher);
        });

        // Enforce hotbar restrictions
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            enforceHotbarRules(player);
        });
    }

    @Override
    public void onInitializeClient() {
        // Handle client-side events like double jump and dash
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player != null) {
                handleDoubleJump(client.player);
                handleDash(client.player);
            }
        });
    }

    // === Command for Toggling Abilities ===
    private void registerToggleCommand(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("toggleAbility")
                .requires(source -> source.hasPermissionLevel(4)) // Admin-only command
                .then(CommandManager.argument("player", StringArgumentType.string())
                        .then(CommandManager.literal("dash")
                                .executes(context -> {
                                    String playerName = StringArgumentType.getString(context, "player");
                                    return toggleAbility(context.getSource(), playerName, "dash");
                                }))
                        .then(CommandManager.literal("doublejump")
                                .executes(context -> {
                                    String playerName = StringArgumentType.getString(context, "player");
                                    return toggleAbility(context.getSource(), playerName, "doublejump");
                                }))));
    }

    private int toggleAbility(ServerCommandSource source, String playerName, String ability) {
        ServerPlayerEntity targetPlayer = source.getServer().getPlayerManager().getPlayer(playerName);

        if (targetPlayer == null) {
            source.sendError(Text.of("Player not found: " + playerName));
            return 0;
        }

        boolean newState;
        if ("dash".equals(ability)) {
            newState = dashEnabled.compute(targetPlayer, (player, state) -> state == null || !state);
            source.sendFeedback(Text.of("Dash for " + playerName + " is now " + (newState ? "enabled" : "disabled")), true);
        } else if ("doublejump".equals(ability)) {
            newState = doubleJumpEnabled.compute(targetPlayer, (player, state) -> state == null || !state);
            source.sendFeedback(Text.of("Double Jump for " + playerName + " is now " + (newState ? "enabled" : "disabled")), true);
        } else {
            source.sendError(Text.of("Invalid ability: " + ability));
            return 0;
        }

        return 1;
    }
    private static void registerGiveItemCommand(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("giveSpecialStick")
                .requires(source -> source.hasPermissionLevel(4)) // Admin-only command
                .then(CommandManager.argument("player", EntityArgumentType.player())
                        .executes(context -> {
                            ServerPlayerEntity targetPlayer = EntityArgumentType.getPlayer(context, "player");
                            return giveSpecialStick(context.getSource(), targetPlayer);
                        })));
    }

    private static int giveSpecialStick(ServerCommandSource source, ServerPlayerEntity player) {
        // Create the special stick
        ItemStack specialStick = new ItemStack(Items.STICK);
        NbtCompound nbt = new NbtCompound();
        nbt.putString(REQUIRED_NBT_KEY, REQUIRED_NBT_VALUE);
        specialStick.setNbt(nbt);
        specialStick.setCustomName(Text.of("Special Stick"));

        // Add to player's inventory
        boolean added = player.getInventory().insertStack(specialStick);

        if (added) {
            source.sendFeedback(Text.of("Gave a Special Stick to " + player.getEntityName()), true);
            return 1;
        } else {
            source.sendError(Text.of(player.getEntityName() + "'s inventory is full!"));
            return 0;
        }
    }









    // === Hotbar Restriction ===
    private void enforceHotbarRules(PlayerEntity player) {
        if (player == null || player.getInventory() == null) return;

        ItemStack slot0Stack = player.getInventory().getStack(0);

        if (!isAllowedItem(slot0Stack)) {
            player.getInventory().setStack(8, ItemStack.EMPTY);
            player.sendMessage(Text.of("Slot 0 only allows a specific item with the correct NBT data!"), true);
        }
    }

    private boolean isAllowedItem(ItemStack stack) {
        if (stack == null || stack.isEmpty() || stack.getItem() != Items.DIAMOND_SWORD) {
            return false;
        }

        if (stack.hasNbt()) {
            NbtCompound nbt = stack.getNbt();
            return nbt != null && REQUIRED_NBT_VALUE.equals(nbt.getString(REQUIRED_NBT_KEY));
        }

        return false;
    }

    // === Double Jump Logic ===
    private void handleDoubleJump(ClientPlayerEntity player) {
        double height = 0.8;
        // Check if double jump is enabled for the player
        if (!doubleJumpEnabled.getOrDefault((ServerPlayerEntity) player, false)) return;

        // Double jump implementation (client-side only)
        if (player.jumpCooldown == 0 && player.getVelocity().y < 0) {
            player.jumpCooldown = 10; // Example cooldown
            player.addVelocity(0, height, 0); // Example jump boost
        }
    }

    // === Dash Logic ===
    private void handleDash(ClientPlayerEntity player) {
        // Check if dash is enabled for the player
        if (!dashEnabled.getOrDefault((ServerPlayerEntity) player, false)) return;

        // Dash implementation (client-side only)
        if (MinecraftClient.getInstance().options.sneakKey.isPressed()) {
            player.addVelocity(player.getRotationVector().x * 1.5, 0, player.getRotationVector().z * 1.5); // Dash forward
        }
    }
}
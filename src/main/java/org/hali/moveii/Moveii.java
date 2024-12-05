package org.hali.moveii;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.HashMap;
import java.util.Map;

public class Moveii implements ModInitializer {
    private static final Map<PlayerEntity, Boolean> dashEnabled = new HashMap<>();
    private static final Map<PlayerEntity, Boolean> doubleJumpEnabled = new HashMap<>();
    private static final Map<PlayerEntity, Double> doubleJumpHeight = new HashMap<>();
    private static final Map<PlayerEntity, Double> dashLength = new HashMap<>();
    private static final Map<PlayerEntity, Long> dashCooldown = new HashMap<>();

    private static final String REQUIRED_NBT_KEY = "AllowedItem";
    private static final String REQUIRED_NBT_VALUE = "Special";

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register(this::registerCommands);
    }

    private void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
        registerToggleAbilityCommand(dispatcher);
        registerSetAbilityValueCommand(dispatcher);
        registerGiveItemCommand(dispatcher);
    }

    private void registerToggleAbilityCommand(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
                CommandManager.literal("toggleAbility")
                        .requires(source -> source.hasPermissionLevel(4)) // Admin command
                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                .then(CommandManager.literal("dash")
                                        .executes(context -> {
                                            ServerPlayerEntity targetPlayer = EntityArgumentType.getPlayer(context, "player");
                                            return toggleAbility(context.getSource(), targetPlayer, "dash");
                                        }))
                                .then(CommandManager.literal("doublejump")
                                        .executes(context -> {
                                            ServerPlayerEntity targetPlayer = EntityArgumentType.getPlayer(context, "player");
                                            return toggleAbility(context.getSource(), targetPlayer, "doublejump");
                                        }))));
    }

    private void registerSetAbilityValueCommand(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
                CommandManager.literal("setAbilityValue")
                        .requires(source -> source.hasPermissionLevel(4)) // Admin command
                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                .then(CommandManager.literal("dashLength")
                                        .then(CommandManager.argument("value", DoubleArgumentType.doubleArg(0.5, 10.0)) // Min: 0.5, Max: 10.0
                                                .executes(context -> {
                                                    ServerPlayerEntity targetPlayer = EntityArgumentType.getPlayer(context, "player");
                                                    double value = DoubleArgumentType.getDouble(context, "value");
                                                    dashLength.put(targetPlayer, value);
                                                    //context.getSource().sendFeedback(Text.of("Set dash length for " + targetPlayer.getEntityName() + " to " + value), true);
                                                    return 1;
                                                })))
                                .then(CommandManager.literal("dashCooldown")
                                        .then(CommandManager.argument("value", IntegerArgumentType.integer(500, 10000)) // Min: 500ms, Max: 10s
                                                .executes(context -> {
                                                    ServerPlayerEntity targetPlayer = EntityArgumentType.getPlayer(context, "player");
                                                    int value = IntegerArgumentType.getInteger(context, "value");
                                                    dashCooldown.put(targetPlayer, (long) value);
                                                    //context.getSource().sendFeedback(Text.of("Set dash cooldown for " + targetPlayer.getEntityName() + " to " + value + "ms"), true);
                                                    return 1;
                                                })))
                                .then(CommandManager.literal("doubleJumpHeight")
                                        .then(CommandManager.argument("value", DoubleArgumentType.doubleArg(0.5, 5.0)) // Min: 0.5, Max: 5.0
                                                .executes(context -> {
                                                    ServerPlayerEntity targetPlayer = EntityArgumentType.getPlayer(context, "player");
                                                    double value = DoubleArgumentType.getDouble(context, "value");
                                                    doubleJumpHeight.put(targetPlayer, value);
                                                    //context.getSource().sendFeedback(Text.of("Set double jump height for " + targetPlayer.getEntityName() + " to " + value), true);
                                                    return 1;
                                                })))));
    }

    private int toggleAbility(ServerCommandSource source, ServerPlayerEntity targetPlayer, String ability) {
        if (targetPlayer == null) {
            source.sendError(Text.of("Player not found."));
            return 0;
        }

        boolean newState;
        if ("dash".equals(ability)) {
            newState = dashEnabled.compute(targetPlayer, (player, state) -> state == null || !state);
            //source.sendFeedback(Text.of("Dash for " + targetPlayer.getEntityName() + " is now " + (newState ? "enabled" : "disabled")), true);
        } else if ("doublejump".equals(ability)) {
            newState = doubleJumpEnabled.compute(targetPlayer, (player, state) -> state == null || !state);
            //source.sendFeedback(Text.of("Double Jump for " + targetPlayer.getEntityName() + " is now " + (newState ? "enabled" : "disabled")), true);
        } else {
            source.sendError(Text.of("Invalid ability: " + ability));
            return 0;
        }

        return 1;
    }

    private void registerGiveItemCommand(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
                CommandManager.literal("giveSpecialStick")
                        .requires(source -> source.hasPermissionLevel(4)) // Admin command
                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                .executes(context -> {
                                    ServerPlayerEntity targetPlayer = EntityArgumentType.getPlayer(context, "player");
                                    if (targetPlayer == null) {
                                        context.getSource().sendError(Text.of("Player not found."));
                                        return 0;
                                    }
                                    return giveSpecialStick(context.getSource(), targetPlayer);
                                })));
    }

    private int giveSpecialStick(ServerCommandSource source, ServerPlayerEntity player) {
        ItemStack specialStick = new ItemStack(Items.STICK);
        NbtCompound nbt = new NbtCompound();
        nbt.putString(REQUIRED_NBT_KEY, REQUIRED_NBT_VALUE);
        specialStick.setNbt(nbt);
        specialStick.setCustomName(Text.of("Special Stick"));

        boolean added = player.getInventory().insertStack(specialStick);

        if (added) {
            //source.sendFeedback(Text.of("Gave a Special Stick to " + player.getEntityName()), true);
            return 1;
        } else {
            source.sendError(Text.of(player.getEntityName() + "'s inventory is full!"));
            return 0;
        }
    }

    public static Map<PlayerEntity, Boolean> getDashEnabled() {
        return dashEnabled;
    }

    public static Map<PlayerEntity, Boolean> getDoubleJumpEnabled() {
        return doubleJumpEnabled;
    }

    public static Map<PlayerEntity, Double> getDoubleJumpHeight() {
        return doubleJumpHeight;
    }

    public static Map<PlayerEntity, Double> getDashLength() {
        return dashLength;
    }

    public static Map<PlayerEntity, Long> getDashCooldown() {
        return dashCooldown;
    }
}

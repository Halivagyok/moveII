package org.hali.moveii;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

public class Moveii implements ModInitializer {
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
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            registerToggleCommand(dispatcher);
        });

        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            enforceHotbarRules(player);
        });
    }

    private void registerToggleCommand(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("toggleAbility")
                .requires(source -> source.hasPermissionLevel(4))
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
            //source.sendError(() -> Text.of("Player not found: " + playerName));
            return 0;
        }

        boolean newState;
        if ("dash".equals(ability)) {
            newState = dashEnabled.compute(targetPlayer, (player, state) -> state == null || !state);
            source.sendFeedback(() -> Text.of("Dash for " + playerName + " is now " + (newState ? "enabled" : "disabled")), true);
        } else if ("doublejump".equals(ability)) {
            newState = doubleJumpEnabled.compute(targetPlayer, (player, state) -> state == null || !state);
            source.sendFeedback(() -> Text.of("Double Jump for " + playerName + " is now " + (newState ? "enabled" : "disabled")), true);
        } else {
            //source.sendError(() -> Text.of("Invalid ability: " + ability));
            return 0;
        }

        return 1;
    }

    private static void registerGiveItemCommand(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("giveSpecialStick")
                .requires(source -> source.hasPermissionLevel(4))
                .then(CommandManager.argument("player", StringArgumentType.word())
                        .executes(context -> {
                            ServerPlayerEntity targetPlayer = context.getSource().getServer().getPlayerManager().getPlayer(StringArgumentType.getString(context, "player"));
                            if (targetPlayer == null) {
                                //context.getSource().sendError(() -> Text.of("Player not found."));
                                return 0;
                            }
                            return giveSpecialStick(context.getSource(), targetPlayer);
                        })));
    }

    private static int giveSpecialStick(ServerCommandSource source, ServerPlayerEntity player) {
        ItemStack specialStick = new ItemStack(Items.STICK);
        NbtCompound nbt = new NbtCompound();
        nbt.putString(REQUIRED_NBT_KEY, REQUIRED_NBT_VALUE);
        specialStick.setNbt(nbt);
        specialStick.setCustomName(Text.of("Special Stick"));

        boolean added = player.getInventory().insertStack(specialStick);

        if (added) {
            source.sendFeedback(() -> Text.of("Gave a Special Stick to " + player.getEntityName()), true);
            return 1;
        } else {
            //source.sendError(() -> Text.of(player.getEntityName() + "'s inventory is full!"));
            return 0;
        }
    }

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
}

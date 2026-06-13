package com.spaceline.client.fabric;

import java.util.ArrayList;
import java.util.List;

import com.spaceline.client.engine.GameState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.util.math.Direction;

/**
 * Minecraft-backed {@link GameState}. Reads live data from {@link MinecraftClient}
 * for the HUD/utility modules. Every accessor guards against being called outside
 * a world by deferring to the interface defaults.
 */
public final class FabricGameState implements GameState {

    private final MinecraftClient client = MinecraftClient.getInstance();

    @Override
    public boolean inGame() {
        return client.player != null && client.world != null;
    }

    @Override
    public String username() {
        return client.player != null ? client.player.getGameProfile().getName() : "Player";
    }

    @Override
    public int fps() {
        return client.getCurrentFps();
    }

    @Override
    public double x() {
        return client.player != null ? client.player.getX() : 0;
    }

    @Override
    public double y() {
        return client.player != null ? client.player.getY() : 0;
    }

    @Override
    public double z() {
        return client.player != null ? client.player.getZ() : 0;
    }

    @Override
    public float yaw() {
        return client.player != null ? client.player.getYaw() : 0;
    }

    @Override
    public float pitch() {
        return client.player != null ? client.player.getPitch() : 0;
    }

    @Override
    public String facing() {
        if (client.player == null) {
            return "North";
        }
        Direction direction = client.player.getHorizontalFacing();
        String axis = switch (direction) {
            case NORTH -> "North (-Z)";
            case SOUTH -> "South (+Z)";
            case EAST -> "East (+X)";
            case WEST -> "West (-X)";
            default -> direction.asString();
        };
        return axis;
    }

    @Override
    public String biome() {
        if (client.world == null || client.player == null) {
            return "Unknown";
        }
        return client.world.getBiome(client.player.getBlockPos())
                .getKey().map(k -> k.getValue().getPath()).orElse("Unknown");
    }

    @Override
    public long pingMillis() {
        if (client.player == null || client.getNetworkHandler() == null) {
            return -1;
        }
        PlayerListEntry entry = client.getNetworkHandler().getPlayerListEntry(client.player.getUuid());
        return entry != null ? entry.getLatency() : -1;
    }

    @Override
    public String serverAddress() {
        if (client.getCurrentServerEntry() != null) {
            return client.getCurrentServerEntry().address;
        }
        return client.isInSingleplayer() ? "singleplayer" : "unknown";
    }

    @Override
    public long worldTime() {
        return client.world != null ? client.world.getTimeOfDay() % 24000 : 0;
    }

    @Override
    public float health() {
        return client.player != null ? client.player.getHealth() : 20f;
    }

    @Override
    public float maxHealth() {
        return client.player != null ? client.player.getMaxHealth() : 20f;
    }

    @Override
    public int food() {
        return client.player != null ? client.player.getHungerManager().getFoodLevel() : 20;
    }

    @Override
    public float saturation() {
        return client.player != null ? client.player.getHungerManager().getSaturationLevel() : 0f;
    }

    @Override
    public List<String> potionEffects() {
        List<String> result = new ArrayList<>();
        if (client.player == null) {
            return result;
        }
        for (StatusEffectInstance effect : client.player.getStatusEffects()) {
            String name = effect.getEffectType().value().getName().getString();
            int seconds = effect.getDuration() / 20;
            result.add(String.format("%s %d:%02d", name, seconds / 60, seconds % 60));
        }
        return result;
    }
}

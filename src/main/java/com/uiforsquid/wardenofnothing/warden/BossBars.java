package com.uiforsquid.wardenofnothing.warden;

import com.uiforsquid.wardenofnothing.config.Config;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.monster.warden.Warden;

/**
 * F1 boss bar: one vanilla boss bar per boss Warden, shown to players inside a cylinder around it
 * (horizontal radius {@code bossBarRangeHorizontal}, vertical half-height {@code bossBarRangeVertical}).
 * Bars are kept in a runtime map (not saved) and updated each server tick, without allocating in the
 * common case.
 */
public final class BossBars {

    private static final Map<UUID, Bar> BARS = new ConcurrentHashMap<>();

    /** A bar plus the config text its name was last built from, so the name is only rebuilt on change. */
    private static final class Bar {
        final ServerBossEvent event;
        String text;

        Bar(String text) {
            this.text = text;
            this.event = new ServerBossEvent(Component.literal(text), Config.BOSS_BAR_COLOR.get(), BossEvent.BossBarOverlay.PROGRESS);
        }
    }

    private BossBars() {
    }

    /** Called each server tick for a boss Warden: refreshes its bar's text, colour, health and viewers. */
    public static void update(Warden warden, ServerLevel level) {
        Bar entry = BARS.computeIfAbsent(warden.getUUID(), id -> new Bar(Config.BOSS_BAR_TEXT.get()));
        ServerBossEvent bar = entry.event;

        String text = Config.BOSS_BAR_TEXT.get();
        if (!text.equals(entry.text)) {
            entry.text = text;
            bar.setName(Component.literal(text));
        }
        bar.setColor(Config.BOSS_BAR_COLOR.get());
        float max = warden.getMaxHealth();
        bar.setProgress(max > 0.0F ? warden.getHealth() / max : 0.0F);

        double hRange = Config.BOSS_BAR_RANGE_HORIZONTAL.get();
        double vRange = Config.BOSS_BAR_RANGE_VERTICAL.get();
        double hRangeSq = hRange * hRange;
        double wx = warden.getX();
        double wy = warden.getY();
        double wz = warden.getZ();

        // Add players in the cylinder, remove the rest of this dimension's players (both are no-ops
        // when nothing changes).
        int inRange = 0;
        for (ServerPlayer player : level.players()) {
            double dx = player.getX() - wx;
            double dz = player.getZ() - wz;
            if (dx * dx + dz * dz <= hRangeSq && Math.abs(player.getY() - wy) <= vRange) {
                bar.addPlayer(player);
                inRange++;
            } else {
                bar.removePlayer(player);
            }
        }
        // More viewers than in-range players means some left the dimension or the server: drop them.
        if (bar.getPlayers().size() != inRange) {
            Set<ServerPlayer> here = new HashSet<>(level.players());
            for (ServerPlayer viewer : new ArrayList<>(bar.getPlayers())) {
                if (!here.contains(viewer)) {
                    bar.removePlayer(viewer);
                }
            }
        }
    }

    /** The players currently shown a Warden's bar (empty if it has none). Read-only; used by the GameTests. */
    public static Collection<ServerPlayer> viewersOf(UUID wardenId) {
        Bar entry = BARS.get(wardenId);
        return entry == null ? List.of() : entry.event.getPlayers();
    }

    /** Hides and drops every bar (server stopping), so nothing leaks into the next world. */
    public static void clear() {
        for (Bar entry : BARS.values()) {
            entry.event.removeAllPlayers();
        }
        BARS.clear();
    }

    /** Removes and hides a Warden's bar (on death, unload, or when it is no longer a boss). */
    public static void remove(UUID wardenId) {
        Bar entry = BARS.remove(wardenId);
        if (entry != null) {
            entry.event.removeAllPlayers();
        }
    }
}

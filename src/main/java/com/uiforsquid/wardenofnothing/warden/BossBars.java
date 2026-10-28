package com.uiforsquid.wardenofnothing.warden;

import com.uiforsquid.wardenofnothing.config.Config;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.monster.warden.Warden;

/**
 * F1 boss bar: one vanilla boss bar per boss Warden, shown to players inside a cylinder around it
 * (horizontal radius {@code bossBarRangeHorizontal}, vertical half-height {@code bossBarRangeVertical}).
 * The bar lives in the Warden's {@link WardenRuntime} (not saved) and is updated each server tick,
 * without allocating in the common case.
 */
public final class BossBars {

    private BossBars() {
    }

    /** Called each server tick for a boss Warden: refreshes its bar's text, colour, health and viewers. */
    public static void update(Warden warden, ServerLevel level) {
        WardenRuntime runtime = WardenRuntime.of(warden);
        String text = Config.BOSS_BAR_TEXT.get();
        ServerBossEvent bar = runtime.bossBar;
        if (bar == null) {
            bar = new ServerBossEvent(Component.literal(text), Config.BOSS_BAR_COLOR.get(), BossEvent.BossBarOverlay.PROGRESS);
            runtime.bossBar = bar;
            runtime.bossBarText = text;
        } else if (!text.equals(runtime.bossBarText)) {
            runtime.bossBarText = text;
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
    public static Collection<ServerPlayer> viewersOf(Warden warden) {
        ServerBossEvent bar = WardenRuntime.of(warden).bossBar;
        return bar == null ? List.of() : bar.getPlayers();
    }

    /** Hides and drops a Warden's bar (it left the world, died, or is no longer a boss). */
    public static void remove(Warden warden) {
        WardenRuntime runtime = WardenRuntime.of(warden);
        if (runtime.bossBar != null) {
            runtime.bossBar.removeAllPlayers();
            runtime.bossBar = null;
            runtime.bossBarText = null;
        }
    }
}

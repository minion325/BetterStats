package me.saif.betterstats.hooks;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.saif.betterstats.BetterStats;
import me.saif.betterstats.statistics.Stat;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PlaceholderAPIHook extends PlaceholderExpansion {

    @Override
    public @NotNull String getIdentifier() {
        return "betterstats";
    }

    @Override
    public @NotNull String getAuthor() {
        return "saif";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0";
    }

    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String params) {
        Stat stat = BetterStats.getAPI().getStat(params);
        if (stat != null) {
            return BetterStats.getAPI().getPlayerStats(player).getFormattedStat(stat);
        } else
            return null;
    }
}

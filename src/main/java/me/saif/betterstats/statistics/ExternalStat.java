package me.saif.betterstats.statistics;

import org.bukkit.OfflinePlayer;

public abstract class ExternalStat extends Stat{

    @Override
    public final boolean isPersistent() {
        return this instanceof LeaderboardStat;
    }

    public abstract double getValue(OfflinePlayer player);

    public abstract void setValue(OfflinePlayer player, double value);

    @Override
    public double getDefaultValue() {
        return 0;
    }
}

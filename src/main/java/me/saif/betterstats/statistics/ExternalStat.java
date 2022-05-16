package me.saif.betterstats.statistics;

import org.bukkit.OfflinePlayer;

public abstract class ExternalStat extends Stat{

    @Override
    public final boolean isPersistent() {
        return false;
    }

    public abstract double getValue(OfflinePlayer player);

    @Override
    public double getDefaultValue() {
        return 0;
    }
}

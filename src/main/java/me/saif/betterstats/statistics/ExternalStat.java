package me.saif.betterstats.statistics;

import org.bukkit.OfflinePlayer;

public abstract class ExternalStat extends Stat{

    public abstract double getValue(OfflinePlayer player);

    @Override
    public double getDefaultValue() {
        return 0;
    }
}

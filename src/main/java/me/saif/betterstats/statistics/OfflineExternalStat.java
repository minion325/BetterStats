package me.saif.betterstats.statistics;

import org.bukkit.OfflinePlayer;

public abstract class OfflineExternalStat extends Stat implements UnmodifiableStat {

    public abstract double getValue(OfflinePlayer player);

}

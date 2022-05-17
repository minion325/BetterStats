package me.saif.betterstats.statistics;

import org.bukkit.entity.Player;

public abstract class OnlineExternalStat extends Stat implements UnmodifiableStat {

    public abstract double getValue(Player player);

}

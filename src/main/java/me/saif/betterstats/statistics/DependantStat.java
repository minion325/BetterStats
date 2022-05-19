package me.saif.betterstats.statistics;

import me.saif.betterstats.player.StatPlayer;

public abstract class DependantStat extends Stat implements UnmodifiableStat {
    public abstract double getValue(StatPlayer statPlayer);
}

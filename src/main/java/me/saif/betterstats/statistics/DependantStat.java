package me.saif.betterstats.statistics;

import me.saif.betterstats.player.StatPlayer;

public abstract class DependantStat extends Stat{

    @Override
    public boolean isPersistent() {
        return this instanceof LeaderboardStat;
    }

    public abstract double getValue(StatPlayer statPlayer);
}

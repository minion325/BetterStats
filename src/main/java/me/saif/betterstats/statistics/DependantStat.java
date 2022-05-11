package me.saif.betterstats.statistics;

import me.saif.betterstats.player.StatPlayer;
import me.saif.betterstats.player.StatPlayerImpl;

public abstract class DependantStat extends Stat{

    @Override
    public boolean isPersistent() {
        return false;
    }

    public abstract double getValue(StatPlayer statPlayer);
}

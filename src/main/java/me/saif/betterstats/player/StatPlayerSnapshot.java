package me.saif.betterstats.player;

import me.saif.betterstats.statistics.Stat;

import java.util.Map;

public class StatPlayerSnapshot {

    private final Map<Stat, Double> actual;
    private final Map<Stat, Double> changes;

    protected StatPlayerSnapshot(StatPlayer statPlayer) {
        this.changes = statPlayer.getChanges();
        this.actual = statPlayer.getData();
        for (Stat stat : this.actual.keySet()) {
            if (actual.get(stat) == null)
                actual.put(stat, statPlayer.getStat(stat));
        }
    }

    public double get(Stat stat) {
        return actual.get(stat);
    }

    public Double getChanges(Stat stat) {
        return changes.get(stat);
    }

}

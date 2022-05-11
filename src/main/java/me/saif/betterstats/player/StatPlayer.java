package me.saif.betterstats.player;

import me.saif.betterstats.statistics.DependantStat;
import me.saif.betterstats.statistics.ExternalStat;
import me.saif.betterstats.statistics.Stat;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class StatPlayer {

    private final UUID uuid;
    private final Map<Stat, Double> statMap = new HashMap<>();
    protected StatPlayer(UUID uuid) {
        this.uuid = uuid;
    }

    protected void addStatToMap (Stat stat, Double value) {
        if (stat instanceof ExternalStat) {
            this.statMap.put(stat, null);
        }
        if (stat instanceof DependantStat) {
            this.statMap.put(stat, null);
        }
        else {
            this.statMap.put(stat, value);
        }
    }

    protected void removeStatFromMap(Stat stat) {
        this.statMap.remove(stat);
    }

    public void setStat(Stat stat, double value) {
        if (!this.statMap.containsKey(stat))
            throw new UnsupportedOperationException("Cannot set a statistic that is not registered");

        if (stat instanceof DependantStat)
            throw new UnsupportedOperationException("Cannot set " + stat.getName());
        if (stat instanceof ExternalStat) {
            ((ExternalStat) stat).setValue(Bukkit.getOfflinePlayer(uuid), value);
            return;
        }
        this.statMap.replace(stat, value);
    }

    public double getStat(Stat stat) {
        if (!statMap.containsKey(stat))
            throw new UnsupportedOperationException("Cannot get a statistic that is not registered");
        if (stat instanceof ExternalStat)
            return ((ExternalStat) stat).getValue(Bukkit.getOfflinePlayer(uuid));
        if (stat instanceof DependantStat)
            return ((DependantStat) stat).getValue(this);
        return statMap.get(stat);
    }

    public String getFormattedStat(Stat stat) {
        return stat.format(this.getStat(stat));
    }

    public void addToStat(Stat stat, double amount) {
        this.setStat(stat, this.getStat(stat) + amount);
    }

    public void removeFromStat(Stat stat, double amount) {
        this.setStat(stat, this.getStat(stat) - amount);
    }

    public void resetStat(Stat stat) {
        this.setStat(stat, stat.getDefaultValue());
    }

    public UUID getUuid() {
        return uuid;
    }

    public OfflinePlayer getPlayer() {
        return Bukkit.getOfflinePlayer(uuid);
    }
}

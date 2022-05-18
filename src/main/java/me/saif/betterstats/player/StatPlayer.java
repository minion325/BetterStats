package me.saif.betterstats.player;

import me.saif.betterstats.statistics.*;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class StatPlayer {

    private UUID uuid;
    private String name;

    //this map contains all the stats
    private Map<Stat, Double> baseValues = new ConcurrentHashMap<>();

    //this map should contain normal stat types
    private Map<Stat, Double> upToDateValues = new HashMap<>();

    protected StatPlayer(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
    }

    protected void addStatToMap(Stat stat, double value) {
        if (stat instanceof DependantStat || stat instanceof OfflineExternalStat)
            baseValues.put(stat, null);
        else if (stat instanceof OnlineExternalStat)
            baseValues.put(stat, value);
        else {
            baseValues.put(stat, value);
            upToDateValues.put(stat, value);
        }
    }

    protected void removeStatFromMap(Stat stat) {
        baseValues.remove(stat);
        upToDateValues.remove(stat);
    }

    protected Map<Stat, Double> getChanges() {
        Map<Stat, Double> changesMap = new HashMap<>();
        this.upToDateValues.forEach((stat, aDouble) -> changesMap.put(stat, aDouble - baseValues.get(stat)));
        return changesMap;
    }

    protected void flushChanges() {
        this.upToDateValues.forEach((stat, aDouble) -> this.baseValues.replace(stat, aDouble));
    }

    protected void flushChanges(Stat... stats) {
        for (Stat stat : stats) {
            if (this.upToDateValues.containsKey(stat))
                this.baseValues.replace(stat, this.upToDateValues.get(stat));
        }
    }

    protected void setBaseValue(Stat stat, double value) {
        if (stat instanceof DependantStat || stat instanceof OfflineExternalStat)
            baseValues.put(stat, null);
        baseValues.put(stat, value);
    }

    protected void addChanges(Map<Stat, Double> changes) {
        for (Stat stat : changes.keySet()) {
            baseValues.computeIfPresent(stat, (statistic, aDouble) -> aDouble + changes.get(statistic));
        }
    }

    public double getStat(Stat stat) {
        checkMapForStat(stat);

        if (stat instanceof OnlineExternalStat)
            if (Bukkit.getPlayer(getUuid()) != null) {
                double value = ((OnlineExternalStat) stat).getValue(Bukkit.getPlayer(getUuid()));
                this.baseValues.replace(stat, value);
                return value;
            } else
                return baseValues.get(stat);
        if (stat instanceof OfflineExternalStat)
            return ((OfflineExternalStat) stat).getValue(getPlayer());
        if (stat instanceof DependantStat)
            return ((DependantStat) stat).getValue(this);
        return upToDateValues.get(stat);
    }

    public void setStat(Stat stat, double value) {
        checkMapForStat(stat);
        checkIfModifiable(stat);

        this.upToDateValues.replace(stat, value);
    }

    public String getFormattedStat(Stat stat) {
        return stat.format(getStat(stat));
    }

    public void addToStat(Stat stat, double value) {
        setStat(stat, getStat(stat) + value);
    }

    public void removeFromStat(Stat stat, double value) {
        setStat(stat, getStat(stat) - value);
    }

    public String getName() {
        return name;
    }

    public UUID getUuid() {
        return uuid;
    }

    public OfflinePlayer getPlayer() {
        return Bukkit.getOfflinePlayer(uuid);
    }

    private void checkMapForStat(Stat stat) {
        if (!baseValues.containsKey(stat))
            throw new UnsupportedOperationException("Cannot access a statistic that is not registered");
    }

    private void checkIfModifiable(Stat stat) {
        if (stat instanceof UnmodifiableStat)
            throw new UnsupportedOperationException("Cannot set unmodifiable stats");
    }
}

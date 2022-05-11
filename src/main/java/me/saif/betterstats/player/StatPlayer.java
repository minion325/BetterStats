package me.saif.betterstats.player;

import me.saif.betterstats.statistics.DependantStat;
import me.saif.betterstats.statistics.ExternalStat;
import me.saif.betterstats.statistics.Stat;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public interface StatPlayer{

    public void setStat(Stat stat, double value);

    public double getStat(Stat stat);

    public String getFormattedStat(Stat stat);

    public void addToStat(Stat stat, double amount);

    public void removeFromStat(Stat stat, double amount);

    public void resetStat(Stat stat);

    public UUID getUuid();

    public OfflinePlayer getPlayer();
}

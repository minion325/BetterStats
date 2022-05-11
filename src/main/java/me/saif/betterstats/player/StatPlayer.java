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

    void setStat(Stat stat, double value);

    double getStat(Stat stat);

    String getFormattedStat(Stat stat);

    void addToStat(Stat stat, double amount);

    void removeFromStat(Stat stat, double amount);

    void resetStat(Stat stat);

    UUID getUuid();

    OfflinePlayer getPlayer();
}

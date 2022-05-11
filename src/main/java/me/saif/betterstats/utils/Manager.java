package me.saif.betterstats.utils;

import me.saif.betterstats.BetterStats;
import org.bukkit.event.Listener;

public class Manager implements Listener {

    private final BetterStats gcStats;

    public Manager(BetterStats stats) {
        this.gcStats = stats;
    }

    public BetterStats getPlugin() {
        return gcStats;
    }

}

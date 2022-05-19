package me.saif.betterstats.leaderboard;

import me.saif.betterstats.BetterStats;
import me.saif.betterstats.statistics.LeaderboardStat;
import me.saif.betterstats.statistics.Stat;
import me.saif.betterstats.utils.Manager;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class LeaderboardManager extends Manager<BetterStats> {

    private Map<Stat, Leaderboard> leaderboardMap = new HashMap<>();
    private Set<LeaderboardStat> stats = new HashSet<>();

    public LeaderboardManager(BetterStats plugin) {
        super(plugin);
    }

    public <T extends Stat & LeaderboardStat> void registerStat(T stat) {
        if (!stat.shouldRegister())
            return;
    }

}

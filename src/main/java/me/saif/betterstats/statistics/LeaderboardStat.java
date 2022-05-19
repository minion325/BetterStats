package me.saif.betterstats.statistics;

public interface LeaderboardStat {

    int numOfTopEntries();

    /**
     * @return Whether this should be registered as a leaderboard stat.
     */
    boolean shouldRegister();

}

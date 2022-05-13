package me.saif.betterstats.commands;

import me.saif.betterstats.BetterStats;
import me.saif.betterstats.player.StatPlayer;
import me.saif.betterstats.statistics.Stat;
import me.saif.betterstats.utils.Callback;
import org.bukkit.ChatColor;
import revxrsal.commands.annotation.*;
import revxrsal.commands.bukkit.annotation.CommandPermission;
import revxrsal.commands.bukkit.core.BukkitActor;

import java.util.List;
import java.util.Locale;

@Command({"betterstats", "bstats"})
public class BetterStatsCommand {

    private static final int PAGE_LENGTH = 6;

    @Subcommand("list")
    @CommandPermission("betterstats.list")
    public void listCommand(BukkitActor actor, @Default("1") @Optional Integer page) {
        List<Stat> statList = BetterStats.getAPI().getRegisteredStats();
        if (page < 1) {
            page = 1;
        } else if (page > ((statList.size() - 1) / PAGE_LENGTH) + 1)
            page = (statList.size() - 1) / PAGE_LENGTH + 1;

        page--;
        actor.getSender().sendMessage(ChatColor.GRAY + "Registered Statistics");

        for (int i = page * PAGE_LENGTH; i < PAGE_LENGTH * (page + 1) && i < statList.size(); i++) {
            Stat stat = statList.get(i);
            actor.getSender().sendMessage(ChatColor.AQUA + stat.getName() + ChatColor.WHITE + " - " + ChatColor.GRAY + stat.getInternalName());
        }
        actor.getSender().sendMessage(ChatColor.GRAY + "Page: " + ChatColor.AQUA + (page + 1) + "/" + (((statList.size() - 1) / PAGE_LENGTH) + 1));
    }

    @Subcommand("parse")
    @AutoComplete("@stats @players")
    public void parseCommand(BukkitActor actor, String statString, @Optional String playerName) {
        Stat toParse = BetterStats.getAPI().getStat(statString.toLowerCase(Locale.ROOT));
        if (toParse == null) {
            actor.getSender().sendMessage(ChatColor.GRAY + "There is no statistic with that name", ChatColor.GRAY + "Use " + ChatColor.AQUA + "/betterstats list" + ChatColor.GRAY + " to get a list of statistics");
            return;
        }

        if (playerName == null || playerName.equals("")) {
            //no player to parse for
            String message = ChatColor.AQUA + toParse.getInternalName() +
                    ChatColor.GRAY + " (" + toParse.getName() + ChatColor.WHITE + ":Default" +
                    ChatColor.GRAY + ")" + ChatColor.WHITE + " -> " + ChatColor.AQUA +
                    toParse.format(toParse.getDefaultValue());
            actor.getSender().sendMessage(message);
            return;
        }

        if (playerName.length() < 3 || playerName.length() > 16) {
            actor.getSender().sendMessage(ChatColor.RED + "Could not find a player by that name!");
            return;
        }

        Callback<StatPlayer> statPlayerCallback = BetterStats.getAPI().getPlayerStats(statString);
        BetterStats.getAPI().getPlayerStats(statString).addResultListener(() -> {
            StatPlayer statPlayer = statPlayerCallback.getResult();

            if (statPlayer == null) {
                actor.getSender().sendMessage(ChatColor.RED + "Could not find a player by that name!");
                return;
            }

            String message = ChatColor.AQUA + toParse.getInternalName() +
                    ChatColor.GRAY + " (" + toParse.getName() + ChatColor.WHITE + ":" + playerName +
                    ChatColor.GRAY + ")" + ChatColor.WHITE + " -> " + ChatColor.AQUA +
                    statPlayer.getFormattedStat(toParse);
            actor.getSender().sendMessage(message);
        });

    }

}

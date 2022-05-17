package me.saif.betterstats.player;

import me.saif.betterstats.BetterStats;
import me.saif.betterstats.data.DataManger;
import me.saif.betterstats.utils.Callback;
import me.saif.betterstats.utils.Manager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class StatPlayerManager extends Manager<BetterStats> {

    private final DataManger dataManger;
    private final Map<String, StatPlayer> nameStatMap = new ConcurrentHashMap<>();
    private final Map<UUID, StatPlayer> uuidStatMap = new ConcurrentHashMap<>();
    private final Map<UUID, Callback<StatPlayer>> uuidCallbackMap = new HashMap<>();
    private final Map<String, Callback<StatPlayer>> nameCallbackMap = new HashMap<>();

    public StatPlayerManager(BetterStats plugin) {
        super(plugin);

        this.dataManger = getPlugin().getDataManger();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onAsyncJoin(AsyncPlayerPreLoginEvent event) {
        if (event.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED)
            return;

        dataManger.saveNameAndUUID(event.getName(), event.getUniqueId());

        UUID uuid = event.getUniqueId();
        String name = event.getName();

        if (this.uuidStatMap.containsKey(uuid)) {
            //data is loaded :D so we just need to check that name is the same
            if (!this.nameStatMap.containsKey(name)) {
                //name is different
                StatPlayer statPlayer = this.uuidStatMap.get(uuid);
                for (String s : this.nameStatMap.keySet()) {
                    if (this.nameStatMap.get(s).equals(statPlayer)) {
                        this.nameStatMap.remove(s);
                        this.nameStatMap.put(name, statPlayer);
                        break;
                    }
                }
            }
            return;
        }
    }
}

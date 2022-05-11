package me.saif.betterstats.utils;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class Callback<T> {

    private final List<Runnable> runnables = new ArrayList<>();
    private final JavaPlugin plugin;
    private T result;
    private boolean results = false;

    public Callback(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public synchronized void addResultListener(Runnable runnable) {
        if (!this.hasResults())
            this.runnables.add(runnable);
        else {
            if (Bukkit.isPrimaryThread())
                runnable.run();
            else
                Bukkit.getScheduler().scheduleSyncDelayedTask(this.plugin, runnable);
        }
    }

    public synchronized void setResult(T result) {
        if (this.hasResults())
            throw new IllegalStateException("Callback already has a result");
        this.result = result;
        this.results = true;

        if (this.runnables.size() == 0)
            return;

        Bukkit.getScheduler().scheduleSyncDelayedTask(this.plugin, () -> {
            for (Runnable runnable : this.runnables) {
                runnable.run();
            }
            this.runnables.clear();
        });
    }

    public synchronized boolean hasResults() {
        return this.results;
    }

    public synchronized T getResult() {
        return result;
    }

    @Override
    public String toString() {
        return "Callback{" +
                ", result=" + result +
                ", results=" + results +
                '}';
    }

}

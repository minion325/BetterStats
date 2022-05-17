package me.saif.betterstats.utils;

import java.util.ArrayList;
import java.util.List;

public class Callback<T> {

    private final List<Runnable> runnables = new ArrayList<>();
    private T result;
    private boolean results = false;

    public synchronized void addResultListener(Runnable runnable) {
        if (!this.hasResults())
            this.runnables.add(runnable);
        else {
            runnable.run();
        }
    }

    /**
     *  Ideally this method should be called from the main thread.
     * @param result Result that is being proviaded to the callback.
     */
    public synchronized void setResult(T result) {
        if (this.hasResults())
            throw new IllegalStateException("Callback already has a result");
        this.result = result;
        this.results = true;

        if (this.runnables.size() == 0)
            return;

        for (Runnable runnable : this.runnables) {
            runnable.run();
        }
        this.runnables.clear();
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

package me.saif.betterstats.statistics;

import java.util.Locale;

public abstract class Stat {

    public abstract String getName();

    public final String getInternalName() {
        return this.getName().toLowerCase(Locale.ROOT).replace(' ', '_');
    }

    public abstract double getDefaultValue();

    public final boolean isPersistent() {
        return (this instanceof OnlineExternalStat || (!(this instanceof DependantStat) && !(this instanceof OfflineExternalStat)));
    }

    public abstract String format(double value);

    public void onRegister() {
    }

    public void onUnregister() {
    }

    public abstract boolean isVisible();

    @Override
    public String toString() {
        return getInternalName();
    }
}

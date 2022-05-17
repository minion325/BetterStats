package me.saif.betterstats.utils;

import java.util.Map;
import java.util.Objects;

public class Pair<W, X> {

    private W w;
    private X x;

    public Pair(W w, X x) {
        this.w = w;
        this.x = x;
    }

    public W getFirst() {
        return w;
    }

    public X getSecond() {
        return x;
    }

    public Pair<W, X> setFirst(W w) {
        this.w = w;
        return this;
    }

    public Pair<W, X> setSecond(X x) {
        this.x = x;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Pair<?, ?> pair)) return false;
        return Objects.equals(w, pair.w) && Objects.equals(x, pair.x);
    }

    @Override
    public int hashCode() {
        return Objects.hash(w, x);
    }

    public static <A, B> Pair<A, B> fromMap(Map<A, B> map) {
        if (map.size() == 0)
            return null;
        if (map.size() > 1)
            throw new IllegalArgumentException("Pair cannot be created from a map with more than one entry");
        A a = map.keySet().iterator().next();
        return new Pair<>(a, map.get(a));
    }
}

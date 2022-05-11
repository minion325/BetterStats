package me.saif.betterstats.utils;

public class Pair<W,X> {

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
}

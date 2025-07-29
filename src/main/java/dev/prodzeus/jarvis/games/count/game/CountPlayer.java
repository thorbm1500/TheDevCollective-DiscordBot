package dev.prodzeus.jarvis.games.count.game;

import org.jetbrains.annotations.NotNull;

public class CountPlayer implements Comparable<CountPlayer> {

    public final long id;
    public int counts = 0;
    public boolean wrongCount = false;

    public long experience = 0L;

    public CountPlayer(final long id) {
        this.id = id;
    }

    @Override
    public int compareTo(@NotNull CountPlayer o) {
        return Integer.compare(counts, o.counts);
    }

}

package dev.prodzeus.jarvis.games.count;

import dev.prodzeus.jarvis.bot.Jarvis;
import dev.prodzeus.jarvis.member.CollectiveMember;
import dev.prodzeus.jarvis.member.MemberManager;
import org.jetbrains.annotations.NotNull;

public class CountPlayer implements Comparable<CountPlayer> {

    public final long id;
    public final long server;
    public int counts = 0;
    public boolean wrongCount = false;
    public long experienceGained;

    public CountPlayer(final long server, final long id) {
        this.id = id;
        this.server = server;
        Jarvis.LOGGER.trace("[Server:{}] [CountPlayer:{}] New instance created.",server,id);
    }

    public void incrementCount() {
        Jarvis.LOGGER.trace("[Server:{}] [CountPlayer:{}] Incrementing count.",server,id);
        this.counts++;
    }

    public CollectiveMember getCollectiveMember() {
        Jarvis.LOGGER.trace("[Server:{}] [CountPlayer] Getting Collective Member of {}",server,id);
        return MemberManager.getCollectiveMember(server,id);
    }

    @Override
    public int compareTo(@NotNull CountPlayer o) {
        if (o.counts == 0) return -1;
        else if (o.counts == 1) return 1;
        return Integer.compare(counts, o.counts);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof CountPlayer oP && id==oP.id;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(id) + Long.hashCode(server);
    }
}

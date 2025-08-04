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
        return Integer.compare(counts, o.counts);
    }

}

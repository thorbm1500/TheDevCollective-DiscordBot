package dev.prodzeus.jarvis.member;

import dev.prodzeus.jarvis.bot.Jarvis;
import dev.prodzeus.logger.Logger;
import dev.prodzeus.logger.SLF4JProvider;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MemberManager {

    private static final Logger LOGGER = SLF4JProvider.get().getLoggerFactory().getLogger("MemberManager");
    private static final ScheduledExecutorService scheduler;
    private static final Set<CollectiveMember> members = new HashSet<>();

    static {
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {
            synchronized (members) {
                final Iterator<CollectiveMember> iterator = members.iterator();
                while (iterator.hasNext()) {
                    final CollectiveMember member = iterator.next();
                    if (!member.isInactive()) {
                        member.resetActivity();
                        continue;
                    }

                    member.save();
                    iterator.remove();
                }
            }
        }, 5, 5, TimeUnit.MINUTES);
    }

    public MemberManager() {
        Jarvis.registerShutdownHook(this::shutdown);
    }

    public static CollectiveMember getCollectiveMember(final long serverId, final long memberId) {
        synchronized (members) {
            for (final CollectiveMember member : members) {
                if (member.id == memberId && member.server == serverId) return member;
            }
            final CollectiveMember member = new CollectiveMember(serverId, memberId);
            members.add(member);
            return member;
        }
    }

    public void shutdown() {
        LOGGER.info("Executing shutdown procedure...");
        scheduler.shutdown();
        synchronized (members) {
            members.forEach(CollectiveMember::save);
            members.clear();
        }
    }
}

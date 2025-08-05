package dev.prodzeus.jarvis.member;

import dev.prodzeus.jarvis.bot.Jarvis;
import dev.prodzeus.logger.Logger;
import dev.prodzeus.logger.SLF4JProvider;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class MemberManager {

    private static final Logger LOGGER = SLF4JProvider.get().getLogger("MemberManager");
    private static final ScheduledExecutorService scheduler;
    private static final Set<CollectiveMember> members = new HashSet<>();

    static {
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {
            final long time = System.currentTimeMillis();
            members.removeAll(
                    members.stream()
                            .filter(CollectiveMember::isInactive)
                            .collect(Collectors.toSet()));
            members.forEach(CollectiveMember::resetActivity);
        }, 5, 5, TimeUnit.MINUTES);
    }

    public MemberManager() {
        Jarvis.registerShutdownHook(this::shutdown);
    }

    public static synchronized CollectiveMember getCollectiveMember(final long serverId, final long memberId) {
        try {
            for (final CollectiveMember member : members) {
                if (member.id == memberId && member.server == serverId) return member;
            }
            final CollectiveMember member = new CollectiveMember(memberId, serverId);
            members.add(member);
            return member;
        } catch (Exception e) {
            LOGGER.error("Failed to get Collective Member! {}", e);
            return null;
        }
    }

    public void shutdown() {
        LOGGER.info("Executing shutdown procedure...");
        members.forEach(CollectiveMember::save);
        members.clear();
        scheduler.shutdown();
    }
}

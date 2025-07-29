package dev.prodzeus.jarvis.member;

import dev.prodzeus.jarvis.bot.Jarvis;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class MemberManager {

    private static final ScheduledExecutorService scheduler;
    private static final Set<CollectiveMember> members = new HashSet<>();

    static {
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {
            final long time = System.currentTimeMillis();
            members.removeAll(
                    members.stream().filter(CollectiveMember::isInactive).collect(Collectors.toSet()));
            members.forEach(CollectiveMember::resetActivity);
        }, 5, 5, TimeUnit.MINUTES);
    }

    public MemberManager() {
        Jarvis.LOGGER.debug("New Member Manager instance created.");
        Jarvis.registerShutdownHook(this::shutdown);
    }

    @NotNull
    public static synchronized CollectiveMember getCollectiveMember(final long memberId, final long serverId) {
        for (final CollectiveMember member : members) {
            if (member.id == memberId && member.server == serverId) return member;
        }
        final CollectiveMember member = new CollectiveMember(memberId,serverId);
        members.add(member);
        return member;
    }

    public static void addExperience(@NotNull final Map<Long,Long> experience) {

    }

    public void shutdown() {
        members.clear();
        scheduler.shutdown();
    }
}

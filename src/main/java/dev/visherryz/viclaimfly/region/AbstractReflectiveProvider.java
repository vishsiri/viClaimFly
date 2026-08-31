package dev.visherryz.viclaimfly.region;

import dev.visherryz.viclaimfly.config.ConfigurationService;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;

abstract class AbstractReflectiveProvider implements ClaimProvider {
    protected final Plugin owner;
    protected final Plugin dependency;
    protected final PermissionPolicy permissions;
    private final ConfigurationService configuration;
    private final AtomicLong lastWarning = new AtomicLong();

    protected AbstractReflectiveProvider(Plugin owner, Plugin dependency, PermissionPolicy permissions,
                                         ConfigurationService configuration) {
        this.owner = owner;
        this.dependency = dependency;
        this.permissions = permissions;
        this.configuration = configuration;
    }

    @Override
    public final ClaimDecision evaluate(Player player) {
        try {
            return evaluateSafely(player);
        } catch (Throwable failure) {
            long now = System.currentTimeMillis();
            long previous = lastWarning.get();
            long intervalMillis = configuration.config().regions().providerWarningIntervalSeconds() * 1_000L;
            if ((intervalMillis == 0L || now - previous >= intervalMillis)
                    && lastWarning.compareAndSet(previous, now)) {
                owner.getLogger().log(Level.WARNING, "Claim provider '" + id() + "' failed; access was denied safely", failure);
            }
            return ClaimDecision.ERROR;
        }
    }

    protected abstract ClaimDecision evaluateSafely(Player player) throws Exception;
}

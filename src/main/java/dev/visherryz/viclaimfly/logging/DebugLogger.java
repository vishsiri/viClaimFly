package dev.visherryz.viclaimfly.logging;

import dev.visherryz.viclaimfly.config.ConfigurationService;
import org.bukkit.plugin.Plugin;

import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public final class DebugLogger {
    private final BooleanSupplier enabled;
    private final Consumer<String> sink;

    public DebugLogger(Plugin plugin, ConfigurationService configuration) {
        this(() -> configuration.config().debug(), message -> plugin.getLogger().info("[debug] " + message));
    }

    DebugLogger(BooleanSupplier enabled, Consumer<String> sink) {
        this.enabled = Objects.requireNonNull(enabled, "enabled");
        this.sink = Objects.requireNonNull(sink, "sink");
    }

    public void log(String message) {
        if (enabled.getAsBoolean()) sink.accept(message);
    }
}

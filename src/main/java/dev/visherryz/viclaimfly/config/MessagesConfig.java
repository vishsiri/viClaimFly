package dev.visherryz.viclaimfly.config;

import java.util.Map;

public record MessagesConfig(String prefix, Map<String, String> messages,
                             String placeholderOn, String placeholderOff, String placeholderWarmup) {
    public String message(String key) {
        return messages.getOrDefault(key, "<red>Missing message: " + key + "</red>");
    }

    public boolean enabled(String key) {
        return !message(key).isBlank();
    }
}

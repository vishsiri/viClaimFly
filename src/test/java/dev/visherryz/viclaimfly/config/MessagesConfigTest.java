package dev.visherryz.viclaimfly.config;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MessagesConfigTest {
    @Test
    void blankMessageDisablesThatNotification() {
        MessagesConfig messages = new MessagesConfig("", Map.of("enabled", "Hello", "disabled", ""),
                "On", "Off", "Warmup");

        assertThat(messages.enabled("enabled")).isTrue();
        assertThat(messages.enabled("disabled")).isFalse();
    }
}

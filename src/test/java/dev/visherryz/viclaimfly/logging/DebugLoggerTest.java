package dev.visherryz.viclaimfly.logging;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;

class DebugLoggerTest {
    @Test
    void disabledLoggerDoesNotPublishMessages() {
        var messages = new ArrayList<String>();
        DebugLogger logger = new DebugLogger(() -> false, messages::add);

        logger.log("hidden");

        assertThat(messages).isEmpty();
    }

    @Test
    void enabledLoggerPublishesMessages() {
        var messages = new ArrayList<String>();
        DebugLogger logger = new DebugLogger(() -> true, messages::add);

        logger.log("flight enabled");

        assertThat(messages).containsExactly("flight enabled");
    }
}

package dev.visherryz.viclaimfly.config;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.junit.jupiter.api.Test;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class ResourceContractTest {
    @Test
    void configContainsEverySupportedProvider() throws Exception {
        ConfigurationNode config = load("config.yml");
        assertThat(config.node("config-version").getInt()).isEqualTo(2);
        assertThat(config.node("commands", "routes", "claimfly").getList(String.class))
                .contains("claimfly", "chocofly");
        assertThat(config.node("permissions", "owner").getString()).isEqualTo("claim.fly");
        assertThat(config.node("notifications", "manual").getBoolean()).isTrue();
        assertThat(config.node("notifications", "auto").getBoolean()).isTrue();
        assertThat(config.node("notifications", "admin").getBoolean()).isTrue();
        assertThat(config.node("slow-falling", "apply-on").getList(String.class))
                .containsExactly("CLAIM_EXIT", "COMBAT");
        assertThat(config.node("integrations", "placeholderapi", "identifier").getString())
                .isEqualTo("claimfly");
        assertThat(config.node("regions", "providers").childrenMap().keySet())
                .containsExactlyInAnyOrder("protectionstones", "lands", "huskclaims", "griefprevention",
                        "redprotect", "griefdefender", "plotsquared", "worldguard");
    }

    @Test
    void everyConfiguredMessageIsValidMiniMessage() throws Exception {
        ConfigurationNode messages = load("messages.yml");
        TagResolver tags = TagResolver.resolver(
                Placeholder.parsed("prefix", messages.node("prefix").getString("")),
                Placeholder.unparsed("seconds", "5"),
                Placeholder.unparsed("player", "Steve"),
                Placeholder.unparsed("providers", "2")
        );
        MiniMessage miniMessage = MiniMessage.miniMessage();
        for (var entry : messages.childrenMap().entrySet()) {
            if (entry.getKey().equals("placeholders")) continue;
            String value = entry.getValue().getString();
            assertThatCode(() -> miniMessage.deserialize(value, tags))
                    .as("message key %s", entry.getKey())
                    .doesNotThrowAnyException();
        }
    }

    @Test
    void pluginDescriptorIsFilteredAndMarksFoliaSupport() throws IOException {
        URL resource = required("plugin.yml");
        String descriptor;
        try (var input = resource.openStream()) {
            descriptor = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
        assertThat(descriptor).contains("folia-supported: true", "main: dev.visherryz.viclaimfly.ViClaimFlyPlugin");
        assertThat(descriptor).contains("claim.fly.join.bypass:");
        assertThat(descriptor).doesNotContain("${version}");
    }

    private ConfigurationNode load(String resource) throws Exception {
        return YamlConfigurationLoader.builder().url(required(resource)).build().load();
    }

    private URL required(String resource) {
        URL value = getClass().getClassLoader().getResource(resource);
        if (value == null) throw new IllegalStateException("Missing test resource: " + resource);
        return value;
    }
}

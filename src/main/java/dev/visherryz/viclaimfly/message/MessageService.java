package dev.visherryz.viclaimfly.message;

import dev.visherryz.viclaimfly.config.ConfigurationService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.command.CommandSender;

public final class MessageService {
    private final ConfigurationService configuration;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public MessageService(ConfigurationService configuration) {
        this.configuration = configuration;
    }

    public void send(CommandSender recipient, String key, TagResolver... resolvers) {
        if (!configuration.messages().enabled(key)) return;
        recipient.sendMessage(render(key, resolvers));
    }

    public Component render(String key, TagResolver... resolvers) {
        TagResolver.Builder tags = TagResolver.builder()
                .resolver(Placeholder.parsed("prefix", configuration.messages().prefix()));
        for (TagResolver resolver : resolvers) tags.resolver(resolver);
        return miniMessage.deserialize(configuration.messages().message(key), tags.build());
    }

    public static TagResolver text(String name, Object value) {
        return Placeholder.unparsed(name, String.valueOf(value));
    }
}

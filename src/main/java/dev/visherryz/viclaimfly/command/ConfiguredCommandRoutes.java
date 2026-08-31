package dev.visherryz.viclaimfly.command;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class ConfiguredCommandRoutes {
    private final Map<String, List<String>> routes;

    private ConfiguredCommandRoutes(Map<String, List<String>> routes) {
        this.routes = Map.copyOf(routes);
    }

    static ConfiguredCommandRoutes create(Map<String, List<String>> routes) {
        return new ConfiguredCommandRoutes(routes);
    }

    String[] rewrite(String[] paths) {
        LinkedHashSet<String> rewritten = new LinkedHashSet<>();
        for (String configuredPath : paths) {
            String path = configuredPath.trim();
            int separator = path.indexOf(' ');
            String root = (separator < 0 ? path : path.substring(0, separator)).toLowerCase(Locale.ROOT);
            String suffix = separator < 0 ? "" : path.substring(separator);
            List<String> labels = routes.get(root);
            if (labels == null) rewritten.add(path);
            else labels.forEach(label -> rewritten.add(label + suffix));
        }
        return rewritten.toArray(String[]::new);
    }

    boolean sameRoutes(ConfiguredCommandRoutes other) {
        return other != null && routes.equals(other.routes);
    }
}

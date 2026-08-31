package dev.visherryz.viclaimfly.region;

import org.bukkit.plugin.Plugin;

/** Narrow public bridge used by optional integrations without exposing implementation details. */
public final class ReflectionBridge {
    private ReflectionBridge() { }

    public static Class<?> type(Plugin plugin, String name) throws ClassNotFoundException {
        return ReflectionAccess.type(plugin, name);
    }

    public static Object call(Object target, String name, Object... arguments) throws ReflectiveOperationException {
        return ReflectionAccess.call(target, name, arguments);
    }

    public static Object callStatic(Class<?> type, String name, Object... arguments) throws ReflectiveOperationException {
        return ReflectionAccess.callStatic(type, name, arguments);
    }

    public static boolean bool(Object target, String name, Object... arguments) throws ReflectiveOperationException {
        return ReflectionAccess.bool(target, name, arguments);
    }
}

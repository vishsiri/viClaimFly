package dev.visherryz.viclaimfly.region;

import org.bukkit.plugin.Plugin;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;

final class ReflectionAccess {
    private ReflectionAccess() { }

    static Class<?> type(Plugin dependency, String name) throws ClassNotFoundException {
        return Class.forName(name, true, dependency.getClass().getClassLoader());
    }

    static Object call(Object target, String name, Object... arguments) throws ReflectiveOperationException {
        Method method = method(target.getClass(), name, false, arguments);
        return method.invoke(target, arguments);
    }

    static Object callStatic(Class<?> type, String name, Object... arguments) throws ReflectiveOperationException {
        Method method = method(type, name, true, arguments);
        return method.invoke(null, arguments);
    }

    static Object field(Object target, String name) throws ReflectiveOperationException {
        Field field = findField(target.getClass(), name);
        field.setAccessible(true);
        return field.get(target);
    }

    static Object staticField(Class<?> type, String name) throws ReflectiveOperationException {
        Field field = findField(type, name);
        field.setAccessible(true);
        return field.get(null);
    }

    static boolean bool(Object target, String name, Object... arguments) throws ReflectiveOperationException {
        return Boolean.TRUE.equals(call(target, name, arguments));
    }

    private static Method method(Class<?> type, String name, boolean requireStatic, Object[] arguments) throws NoSuchMethodException {
        for (Method candidate : type.getMethods()) {
            if (!candidate.getName().equals(name) || candidate.getParameterCount() != arguments.length) continue;
            if (requireStatic != Modifier.isStatic(candidate.getModifiers())) continue;
            Class<?>[] parameters = candidate.getParameterTypes();
            boolean compatible = true;
            for (int index = 0; index < parameters.length; index++) {
                if (!compatible(parameters[index], arguments[index])) {
                    compatible = false;
                    break;
                }
            }
            if (compatible) {
                candidate.setAccessible(true);
                return candidate;
            }
        }
        throw new NoSuchMethodException(type.getName() + '#' + name + Arrays.toString(arguments));
    }

    private static boolean compatible(Class<?> parameter, Object argument) {
        if (argument == null) return !parameter.isPrimitive();
        Class<?> boxed = parameter.isPrimitive() ? box(parameter) : parameter;
        return boxed.isAssignableFrom(argument.getClass());
    }

    private static Class<?> box(Class<?> primitive) {
        if (primitive == boolean.class) return Boolean.class;
        if (primitive == byte.class) return Byte.class;
        if (primitive == short.class) return Short.class;
        if (primitive == int.class) return Integer.class;
        if (primitive == long.class) return Long.class;
        if (primitive == float.class) return Float.class;
        if (primitive == double.class) return Double.class;
        if (primitive == char.class) return Character.class;
        return primitive;
    }

    private static Field findField(Class<?> type, String name) throws NoSuchFieldException {
        Class<?> current = type;
        while (current != null) {
            try {
                return current.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchFieldException(type.getName() + '#' + name);
    }
}

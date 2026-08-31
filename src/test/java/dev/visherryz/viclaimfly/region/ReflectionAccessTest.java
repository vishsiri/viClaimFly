package dev.visherryz.viclaimfly.region;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReflectionAccessTest {
    @Test
    void resolvesPrimitiveAndAssignableParameters() throws Exception {
        Fixture fixture = new Fixture();
        assertThat(ReflectionAccess.call(fixture, "combine", 4, " blocks")).isEqualTo("4 blocks");
    }

    @Test
    void resolvesInheritedPrivateFields() throws Exception {
        assertThat(ReflectionAccess.field(new ChildFixture(), "secret")).isEqualTo("kept");
    }

    @Test
    void rejectsAnIncompatibleOverload() {
        assertThatThrownBy(() -> ReflectionAccess.call(new Fixture(), "combine", "four", " blocks"))
                .isInstanceOf(NoSuchMethodException.class);
    }

    public static class Fixture {
        public String combine(int value, CharSequence suffix) { return value + suffix.toString(); }
    }

    private static class ParentFixture { private final String secret = "kept"; }
    private static final class ChildFixture extends ParentFixture { }
}

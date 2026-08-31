import java.util.zip.ZipFile

plugins {
    java
    jacoco
    id("com.gradleup.shadow") version "9.3.2"
    id("xyz.jpenilla.run-paper") version "3.0.2"
}

group = "dev.visherryz"
version = "1.1.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.extendedclip.com/releases/")
}

dependencies {
    compileOnly("dev.folia:folia-api:1.21.11-R0.1-SNAPSHOT")
    compileOnly("me.clip:placeholderapi:2.12.3")

    implementation("io.github.revxrsal:lamp.common:4.0.0-rc.18")
    implementation("io.github.revxrsal:lamp.bukkit:4.0.0-rc.18")
    implementation("org.spongepowered:configurate-yaml:4.2.0")

    compileOnly("org.projectlombok:lombok:1.18.46")
    annotationProcessor("org.projectlombok:lombok:1.18.46")
    testCompileOnly("org.projectlombok:lombok:1.18.46")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.46")

    testImplementation(platform("org.junit:junit-bom:6.0.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core:3.27.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
    withSourcesJar()
}

jacoco.toolVersion = "0.8.13"

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(21)
    options.compilerArgs.addAll(listOf("-parameters", "-Xlint:deprecation"))
}

tasks.processResources {
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") { expand("version" to project.version) }
}

tasks.test {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport)
}

tasks.shadowJar {
    archiveClassifier.set("")
    mergeServiceFiles()
    exclude("module-info.class", "META-INF/versions/**")
    dependencies {
        exclude(dependency("net.kyori:adventure-api:.*"))
        exclude(dependency("net.kyori:adventure-key:.*"))
        exclude(dependency("net.kyori:adventure-text-serializer-.*:.*"))
        exclude(dependency("net.kyori:examination-.*:.*"))
    }
    relocate("revxrsal.commands", "dev.visherryz.viclaimfly.libs.lamp")
    relocate("org.spongepowered.configurate", "dev.visherryz.viclaimfly.libs.configurate")
    relocate("org.yaml.snakeyaml", "dev.visherryz.viclaimfly.libs.snakeyaml")
}

tasks.jar { enabled = false }
tasks.build { dependsOn(tasks.shadowJar, "verifyShadowJar") }

val verifyShadowJar by tasks.registering {
    dependsOn(tasks.shadowJar)
    doLast {
        ZipFile(tasks.shadowJar.get().archiveFile.get().asFile).use { zip ->
            check(zip.getEntry("dev/visherryz/viclaimfly/ViClaimFlyPlugin.class") != null)
            check(zip.getEntry("dev/visherryz/viclaimfly/libs/lamp/Lamp.class") != null)
            check(zip.getEntry("dev/visherryz/viclaimfly/libs/configurate/ConfigurationNode.class") != null)
            check(zip.getEntry("net/kyori/adventure/text/Component.class") == null)
            check(zip.getEntry("org/bukkit/Bukkit.class") == null)
            check(zip.getEntry("me/clip/placeholderapi/PlaceholderAPI.class") == null)
        }
    }
}

tasks.runServer {
    minecraftVersion("1.21.11")
    jvmArgs("-Xms1G", "-Xmx2G")
}

runPaper.folia.registerTask {
    minecraftVersion("1.21.11")
    runDirectory = rootDir.resolve("run-folia")
    jvmArgs("-Xms1G", "-Xmx2G")
}

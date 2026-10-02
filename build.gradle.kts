plugins {
    id("java")
    id("maven-publish")
    id("com.gradleup.shadow") version "9.2.2"
}

group = "pack.utilities"
version = "1.0.0"

repositories {
    mavenCentral()
    mavenLocal()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:6.0.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    //lombok
    compileOnly("org.projectlombok:lombok:1.18.38")
    annotationProcessor("org.projectlombok:lombok:1.18.38")
    testCompileOnly("org.projectlombok:lombok:1.18.38")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.38")
}

tasks.test {
    useJUnitPlatform()
}

tasks.shadowJar {
    // Shadow strips the root module-info.class, which would downgrade this artifact from
    // the explicit `module pack.utilities` to an automatic one. Only the multi-release
    // copies need excluding.
    exclude("META-INF/versions/*/module-info.class")

    manifest {
        attributes["Main-Class"] = "pack.utilities.Main"
        // Fallback for consumers still resolving a jar published before module-info.java
        // existed. Without this, the JitPack filename pack-utilities-v1.0.0.jar derives the
        // invalid module name "pack.utilities.v1.0.0" and cannot enter a module graph.
        attributes["Automatic-Module-Name"] = "pack.utilities"
    }

    archiveBaseName.set("utilities-pack")
    // MUST NOT be "" — that collides with the plain `jar` output. Both tasks would write
    // build/libs/utilities-pack-1.0.0.jar, and whichever ran last would win, silently
    // publishing the shadow jar (automatic module) instead of the modular plain jar.
    archiveClassifier.set("all")
    archiveVersion.set(project.version.toString())
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

// Publish under the exact coordinates JitPack serves, so that consumers declaring
//   implementation("com.github.eliezer-dev-software-enginner:pack-utilities:v1.0.0")
// resolve the MODULAR jar from mavenLocal without any change to their build scripts.
// JitPack builds from git tags, so this only takes effect locally until the fix is pushed.
//
// Publishes components["java"] (the plain jar) rather than shadowJar on purpose: shadow
// strips the root module-info.class, which would downgrade the artifact from the explicit
// `module pack.utilities` back to an automatic module. This library has zero runtime
// dependencies, so the plain jar and the shadow jar are content-equivalent.
publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            groupId = "com.github.eliezer-dev-software-enginner"
            artifactId = "pack-utilities"
            version = "v1.0.0"
            from(components["java"])
            pom {
                name.set("pack-utilities")
                description.set("Utilities used across the Megalodonte ecosystem")
            }
        }
    }
}
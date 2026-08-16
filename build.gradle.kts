plugins {
    java
}

group = "dev.worldgenerator"
version = "0.7.0-SNAPSHOT"
val pluginVersion = version

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.1.2.build.74-stable")
    testImplementation(platform("org.junit:junit-bom:5.12.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

tasks.test {
    useJUnitPlatform()
}

tasks.processResources {
    filesMatching("plugin.yml") {
        expand("version" to pluginVersion)
    }
}

tasks.compileJava {
    options.compilerArgs.add("-Xlint:deprecation")
}

tasks.register<JavaExec>("renderTerrainPreviews") {
    group = "verification"
    description = "Renders deterministic v0.7 terrain and height-map PNG previews."
    dependsOn(tasks.testClasses)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("dev.worldgenerator.terrain.TerrainPreviewRenderer")
    args(layout.buildDirectory.dir("reports/terrain").get().asFile.absolutePath)
}

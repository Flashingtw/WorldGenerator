plugins {
    java
}

group = "dev.worldgenerator"
version = "0.8.7-SNAPSHOT"
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
    inputs.property("pluginVersion", pluginVersion)
    filesMatching("plugin.yml") {
        expand("version" to pluginVersion)
    }
}

tasks.compileJava {
    options.compilerArgs.add("-Xlint:deprecation")
}

tasks.register<JavaExec>("renderTerrainPreviews") {
    group = "verification"
    description = "Renders deterministic terrain, hydrology, road, height, and surface previews."
    dependsOn(tasks.testClasses)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("dev.worldgenerator.terrain.TerrainPreviewRenderer")
    args(layout.buildDirectory.dir("reports/terrain").get().asFile.absolutePath)
}

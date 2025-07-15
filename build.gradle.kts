plugins {
    id("java")
}

group = "dev.prodzeus"
version = "0.1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    implementation("com.github.thorbm1500:UtilityLib:e8fbf74")
    implementation("net.dv8tion:JDA:${project.properties["jda_version"]}") {
        exclude(module = "opus-java")
        exclude(module = "tink")
    }
    implementation("commons-cli:commons-cli:1.9.0")
    compileOnly("org.projectlombok:lombok:1.18.38")
    annotationProcessor("org.projectlombok:lombok:1.18.38")
    implementation("org.reflections:reflections:0.10.2")
}

tasks.withType<Jar>() {
    manifest {
        attributes(
            "MainClass" to "dev.prodzeus.tdcdb.Main"
        )
    }
}
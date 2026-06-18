import java.util.Properties

val localProperties = Properties()

val localFile = rootProject.file(
    "local.properties"
)

if (localFile.exists()) {

    localFile.inputStream().use {

        localProperties.load(it)

    }

}

val pluginsPath =

    localProperties.getProperty(
        "SERVER_PLUGINS_PATH"
    )

val autoDeploy =

    localProperties.getProperty(
        "AUTO_DEPLOY"
    )?.toBoolean() ?: false

plugins {
    kotlin("jvm") version "2.0.21"
    id("com.gradleup.shadow") version "8.3.0"
    id("xyz.jpenilla.run-paper") version "2.3.1"

    //id("io.papermc.paperweight.userdev") version "2.0.0-beta.19"
}

group = "com.nikoneko"
version = "1.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/") {
        name = "papermc-repo"
    }
    maven {
        name = "citizens-repo"
        url = uri("https://maven.citizensnpcs.co/repo")
    }

}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21-R0.1-SNAPSHOT")
    compileOnly(files("libs/Citizens-2.0.35-b3598.jar"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
}

tasks {
  runServer {
    // Configure the Minecraft version for our task.
    // This is the only required configuration besides applying the plugin.
    // Your plugin's jar (or shadowJar if present) will be used automatically.
    minecraftVersion("1.21")
  }
}

java {
    toolchain {
        languageVersion.set(
            JavaLanguageVersion.of(21)
        )
    }
}

kotlin {
    jvmToolchain(21)
}

tasks.build {
    dependsOn("shadowJar")
}

tasks.jar {
    doLast {
        if (
            autoDeploy &&
            pluginsPath != null
        ) {
            copy {
                from(tasks.jar)
                into(pluginsPath)
            }
        }
    }
}

tasks.processResources {
    val props = mapOf("version" to version)
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand(props)
    }
}

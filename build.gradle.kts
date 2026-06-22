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

val pluginsPath = localProperties.getProperty("SERVER_PLUGINS_PATH")!!

val autoDeploy = localProperties.getProperty("AUTO_DEPLOY")?.toBoolean() ?: false

plugins {
    kotlin("jvm") version "2.0.21"
    id("com.gradleup.shadow") version "8.3.0"
    id("xyz.jpenilla.run-paper") version "2.3.1"
}

group = "com.nikoneko"

version = "1.0"

repositories {
    mavenCentral()
    maven(
        "https://repo.papermc.io/repository/maven-public/"
    ) {
        name = "papermc-repo"
    }
    
    maven(
        "https://repo.infernalsuite.com/repository/maven-snapshots/"
    ) {
        name = "asp-repo"
    }

    maven {
        name = "citizens-repo"
        url = uri(
            "https://maven.citizensnpcs.co/repo"
        )
    }
}

dependencies {
    compileOnly(
        "com.infernalsuite.asp:api:4.+"
    )
    compileOnly(
        "io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT"
    )

    compileOnly("net.citizensnpcs:citizens-main:2.0.42-SNAPSHOT") { isTransitive = false }

    implementation(
        kotlin(
            "stdlib"
        )
    )
    implementation("com.google.code.gson:gson:2.10.1")
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

tasks {
    runServer {
        minecraftVersion(
            "1.21"
        )

    }

    jar {
        doLast {
            if (
                autoDeploy
            ) {
                copy {
                    from(
                        archiveFile.get().asFile
                    )
                    into(
                        pluginsPath
                    )
                }
            }
        }
    }

    shadowJar{
        archiveClassifier.set("")
    }

    processResources {
        val props = mapOf(
            "version" to version
        )
        inputs.properties(props)
        filteringCharset = "UTF-8"
        filesMatching(
            "plugin.yml"
        ) {
            expand(props)
        }
    }

    build{
        finalizedBy("deployPlugin")
    }
}

tasks.register<Copy>("deployPlugin") {

    dependsOn(tasks.shadowJar)
    onlyIf { autoDeploy }
    from(
        tasks.shadowJar.flatMap {
            it.archiveFile
        }
    )
    into(pluginsPath)
}
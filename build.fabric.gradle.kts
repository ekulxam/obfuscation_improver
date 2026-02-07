import java.io.BufferedReader
import java.io.FileReader
import org.gradle.jvm.tasks.Jar

plugins {
    id("net.fabricmc.fabric-loom-remap")
    id("maven-publish")
    id("com.modrinth.minotaur")
    kotlin("jvm")
    id("com.google.devtools.ksp")
    id("dev.kikugie.fletching-table.fabric")
}

version = "${project.property("mod_version")}+${stonecutter.current.version}"
group = project.property("maven_group") as String
val minecraft : String = if (hasProperty("deps.minecraft")) project.property("deps.minecraft") as String
else stonecutter.current.version

base.archivesName = project.property("archives_base_name") as String

repositories {
	// Add repositories to retrieve artifacts from in here.
	// You should only use this when depending on other mods because
	// Loom adds the essential maven repositories to download Minecraft and libraries from automatically.
	// See https://docs.gradle.org/current/userguide/declaring_repositories.html
	// for more information about repositories.
	maven ("https://maven.terraformersmc.com")
    maven ("https://maven.parchmentmc.org")
}

fabricApi {
    configureDataGeneration {
        client = true
    }
}

dependencies {
    // To change the versions see the gradle.properties file
    minecraft("com.mojang:minecraft:${minecraft}")

    // apply mappings if obfuscated
    @Suppress("UnstableApiUsage")
    mappings (loom.layered {
        officialMojangMappings()
        if (hasProperty("deps.parchment")) {
            parchment("org.parchmentmc.data:parchment-${minecraft}:${property("deps.parchment")}@zip")
        }
    })

	modImplementation("net.fabricmc:fabric-loader:${property("deps.loader_version")}")

	// Fabric API. This is technically optional, but you probably want it anyway.
	modImplementation("net.fabricmc.fabric-api:fabric-api:${property("deps.fabric_api")}")

	modRuntimeOnly("com.terraformersmc:modmenu:${property("deps.modmenu_version")}")
}

stonecutter {
    replacements.string {
        direction = eval(minecraft, ">1.21.10")
        replace("ResourceLocation", "Identifier")
    }

    replacements.string {
        direction = eval(current.version, ">1.21.8")
        replace("PlayerRenderer", "AvatarRenderer")
    }
}

fletchingTable {
	mixins.create("main") {
		// Default matches the default value in the annotation
		mixin("default", "obfuscation_improver.mixins.json") {
            env("CLIENT")
        }
    }
    mixins.all {
        automatic = false
    }
}

tasks.processResources {
    val modVersion = project.version
    val minecraftVersion = minecraft
    inputs.property("version", modVersion)
    inputs.property("minecraft", minecraftVersion)

    filesMatching("fabric.mod.json") {
        expand(
            mapOf(
                "version" to modVersion,
                "minecraft" to minecraftVersion
            )
        )
    }
}

tasks.named("build") {
    finalizedBy("autoVersionChangelog")
}

tasks.register("autoVersionChangelog") {
    doLast {
        val changelog = File("changelog.md")
        val reader = BufferedReader(FileReader(changelog))
        val lines = reader.readLines().toMutableList()
        val title = "Obfuscated Text Improver ${project.property("mod_version")}"
        lines[0] = title
        changelog.bufferedWriter().use { writer ->
            for (i in 0..<lines.size) {
                writer.write(lines[i])
                if (i != lines.size - 1) {
                    writer.newLine()
                }
            }
        }
        println("Changelog header successfully replaced as $title")
    }
}

loom {
    runConfigs.all {
        ideConfigGenerated(true)
        runDir = "../../run"
    }

    runConfigs["client"].apply {
        programArgs("--username=Survivalblock", "--uuid=c45e97e6-94ef-42da-8b5e-0c3209551c3f")
    }

    fabricModJsonPath = rootProject.file("src/main/resources/fabric.mod.json")
}

tasks.withType<JavaCompile>().configureEach {
    val java = if (stonecutter.eval(minecraft, ">=26")) {
        25
    } else if (stonecutter.eval(minecraft, ">=1.20.5")) {
        21
    } else {
        17
    }
	options.release.set(java)
}

java {
    // Loom will automatically attach sourcesJar to a RemapSourcesJar task and to the "build" task
    // if it is present.
    // If you remove this line, sources will not be generated.
    withSourcesJar()

    val java = if (stonecutter.eval(minecraft, ">=26")) {
        JavaVersion.VERSION_25
    } else if (stonecutter.eval(minecraft, ">=1.20.5")) {
        JavaVersion.VERSION_21
    } else {
        JavaVersion.VERSION_17
    }

    targetCompatibility = java
    sourceCompatibility = java
}

tasks.jar {
    inputs.property("archivesName", project.base.archivesName)

    from("LICENSE") {
        rename { "${it}_${base.archivesName}"}
    }
}

// configure the maven publication
publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifactId = project.property("archives_base_name") as String
            from(components["java"])
        }
    }

    // See https://docs.gradle.org/current/userguide/publishing_maven.html for information on how to set up publishing.
    repositories {
        // Add repositories to publish to here.
        // Notice: This block does NOT have the same function as the block in the top level.
        // The repositories here will be used for publishing your artifact, not for
        // retrieving dependencies.
    }
}

modrinth {
    token = providers.environmentVariable("MODRINTH_TOKEN")
    projectId = project.base.archivesName
    version = project.version
    uploadFile.set(tasks.named<Jar>("remapJar").get().archiveFile)
    additionalFiles.add(tasks.named<Jar>("sourcesJar").get().archiveFile)
    gameVersions.addAll("${project.property("deps.compatibleVersions")}".split(", ").toList())
    loaders.addAll("${project.property("deps.compatibleLoaders")}".split(", ").toList())
    changelog = rootProject.file("changelog.md").readText()
    syncBodyFrom = "<!--DO NOT EDIT MANUALLY: synced from gh readme-->\n" + rootProject.file("README.md").readText()
}
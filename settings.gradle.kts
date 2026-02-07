pluginManagement {
	repositories {
		maven("https://maven.fabricmc.net/") { name = "Fabric" }
		mavenCentral()
		gradlePluginPortal()
		maven("https://maven.kikugie.dev/releases") { name = "KikuGie Releases" }
		maven("https://maven.kikugie.dev/snapshots") { name = "KikuGie Snapshots" }
	}
}

plugins {
	id("dev.kikugie.stonecutter") version "0.7.10"
}

stonecutter {
	kotlinController = true

	// Subproject configuration
	create(rootProject) {
        fun match(version: String, vararg loaders: String) = loaders.forEach {
            if (it == "fabric" && stonecutter.eval(version, ">=26")) {
                version("$version-$it", version).buildscript = "build.fabric_noremap.gradle.kts"
            } else {
                version("$version-$it", version).buildscript = "build.$it.gradle.kts"
            }
        }

        match("1.21.1", "fabric")
		vcsVersion = "1.21.1-fabric"
	}
}
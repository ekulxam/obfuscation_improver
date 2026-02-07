plugins {
    id("dev.kikugie.stonecutter")
    id("net.fabricmc.fabric-loom-remap") version "1.14-SNAPSHOT" apply false
    id("net.fabricmc.fabric-loom") version "1.15.2" apply false
    id("com.modrinth.minotaur") version "2.+" apply false
    kotlin("jvm") version "2.3.0" apply false
    id("com.google.devtools.ksp") version "2.3.0" apply false
    id("dev.kikugie.fletching-table.fabric") version "0.1.0-alpha.22" apply false
}

stonecutter active "1.21.11-fabric"
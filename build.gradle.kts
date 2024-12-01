import org.apache.commons.lang3.SystemUtils
import java.io.FileOutputStream
import java.net.URL

plugins {
    idea
    java
    id("gg.essential.loom") version "0.10.0.+"
    id("dev.architectury.architectury-pack200") version "0.1.3"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    kotlin("jvm") version "2.0.0-Beta1"
    kotlin("plugin.serialization") version "2.0.0-Beta1"
    id("net.kyori.blossom") version "1.3.1"
}

//Constants:

val baseGroup: String by project
val mcVersion: String by project
val version: String by project
val mixinGroup = "$baseGroup.mixin"
val modID: String by project
val transformerFile = file("src/main/resources/accesstransformer.cfg")

val requiredOdin = project.findProperty("requiredOdin") as String
val requiredOdinVersion = requiredOdin.substringAfterLast("-").substringBefore(".jar")

blossom {
    replaceToken("@VER@", version)
    replaceToken("@REQUIREDODINVERSION@", requiredOdinVersion)
    replaceToken("@MODVERSION@", version)
}


tasks.register("downloadOdin") {
    val downloadUrl = "https://github.com/odtheking/Odin/releases/download/${requiredOdinVersion}/${requiredOdin}"
    val targetFile = file("build/resources/Odin")

    doLast {
        targetFile.mkdirs()

        URL(downloadUrl).openStream().use { input ->
            FileOutputStream(File(targetFile, requiredOdin)).use { output ->
                input.copyTo(output)
            }
        }
    }
}

tasks.named("compileJava") {
    dependsOn("downloadOdin")
}

tasks.named("compileKotlin") {
    dependsOn("downloadOdin")
}


// Toolchains:
java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(8))
}

// Minecraft configuration:
loom {
    log4jConfigs.from(file("log4j2.xml"))
    launchConfigs {
        "client" {
            // If you don't want mixins, remove these lines
            property("mixin.debug", "true")
            arg("--tweakClass", "org.spongepowered.asm.launch.MixinTweaker")
        }
    }
    runConfigs {
        "client" {
            if (SystemUtils.IS_OS_MAC_OSX) {
                // This argument causes a crash on macOS
                vmArgs.remove("-XstartOnFirstThread")
            }
        }
        remove(getByName("server"))
    }
    forge {
        pack200Provider.set(dev.architectury.pack200.java.Pack200Adapter())
        // If you don't want mixins, remove this lines
        mixinConfig("mixins.$modID.json")
        if (transformerFile.exists()) {
            println("Installing access transformer")
            accessTransformer(transformerFile)
        }
    }
    // If you don't want mixins, remove these lines
    mixin {
        defaultRefmapName.set("mixins.$modID.refmap.json")
    }
}

tasks.compileJava {
    dependsOn(tasks.processResources)
}

sourceSets.main {
    output.setResourcesDir(sourceSets.main.flatMap { it.java.classesDirectory })
    java.srcDir(layout.projectDirectory.dir("src/main/kotlin"))
    kotlin.destinationDirectory.set(java.destinationDirectory)
}

// Dependencies:

repositories {
    mavenCentral()
    maven("https://repo.spongepowered.org/maven/")
    maven("https://jitpack.io/")
    // If you don't want to log in with your real minecraft account, remove this line
    maven("https://pkgs.dev.azure.com/djtheredstoner/DevAuth/_packaging/public/maven/v1")
}

val shadowImpl: Configuration by configurations.creating {
    configurations.implementation.get().extendsFrom(this)
}

dependencies {
    minecraft("com.mojang:minecraft:1.8.9")
    mappings("de.oceanlabs.mcp:mcp_stable:22-1.8.9")
    forge("net.minecraftforge:forge:1.8.9-11.15.1.2318-1.8.9")
    implementation(files("build/resources/Odin/${requiredOdin}"))

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")

    implementation("com.github.Stivais:Commodore:3f4a14b1cf")

    shadowImpl(kotlin("stdlib-jdk8"))

    // If you don't want mixins, remove these lines
    shadowImpl("org.spongepowered:mixin:0.7.11-SNAPSHOT") {
        isTransitive = false
    }
    annotationProcessor("org.spongepowered:mixin:0.8.5-SNAPSHOT")

    // If you don't want to log in with your real minecraft account, remove this line
    runtimeOnly("me.djtheredstoner:DevAuth-forge-legacy:1.2.1")
    implementation(kotlin("stdlib-jdk8"))

}

// Tasks:

tasks.withType(JavaCompile::class) {
    options.encoding = "UTF-8"
}

tasks.withType(org.gradle.jvm.tasks.Jar::class) {
    archiveBaseName.set(modID)
    manifest.attributes.run {
        this["FMLCorePluginContainsFMLMod"] = "true"
        this["ForceLoadAsMod"] = "true"

        // If you don't want mixins, remove these lines
        this["TweakClass"] = "org.spongepowered.asm.launch.MixinTweaker"
        this["MixinConfigs"] = "mixins.$modID.json"
        if (transformerFile.exists())
            this["FMLAT"] = "${modID}_at.cfg"
    }
}

tasks.processResources {
    inputs.property("version", project.version)
    inputs.property("mcversion", mcVersion)
    inputs.property("modID", modID)
    inputs.property("basePackage", baseGroup)

    filesMatching(listOf("mcmod.info", "mixins.$modID.json")) {
        expand(inputs.properties)
    }

    rename("accesstransformer.cfg", "META-INF/${modID}_at.cfg")
}


val remapJar by tasks.named<net.fabricmc.loom.task.RemapJarTask>("remapJar") {
    archiveClassifier.set("")
    from(tasks.shadowJar)
    input.set(tasks.shadowJar.get().archiveFile)
}

tasks.jar {
    archiveClassifier.set("without-deps")
    destinationDirectory.set(layout.buildDirectory.dir("intermediates"))
}

tasks.shadowJar {
    destinationDirectory.set(layout.buildDirectory.dir("intermediates"))
    archiveClassifier.set("non-obfuscated-with-deps")
    configurations = listOf(shadowImpl)
    doLast {
        configurations.forEach {
            println("Copying dependencies into mod: ${it.files}")
        }
    }

    // If you want to include other dependencies and shadow them, you can relocate them in here
    fun relocate(name: String) = relocate(name, "$baseGroup.deps.$name")
}

//tasks.register<JavaExec>("obfuscate") {
//    group = "build"
//    description = "Obfuscates the mod using ProGuard"
//
//    val proguardJar = file("libs/proguard.jar")
//    val inputJar = file("${layout.buildDirectory.get()}/libs/$modID-${project.version}.jar")
//    val outputJar = file("${layout.buildDirectory.get()}/libs/$modID-obfuscated.jar")
//    val rulesFile = file("proguard-rules.pro")
//    val jrtFsJar = file("${System.getProperty("java.home")}/lib/jrt-fs.jar")
//
//    mainClass.set("proguard.ProGuard")
//    classpath = files(proguardJar)
//
//    args(
//        "-injars", inputJar.absolutePath,
//        "-outjars", outputJar.absolutePath,
//        "-libraryjars", jrtFsJar.absolutePath,
//        "-include", rulesFile.absolutePath
//    )
//}
//
//tasks.remapJar {
//    finalizedBy("obfuscate") // Ensure obfuscation happens after remapJar
//}

//tasks.named("build") {
//    dependsOn("obfuscate")
//}

tasks.assemble.get().dependsOn(tasks.remapJar)

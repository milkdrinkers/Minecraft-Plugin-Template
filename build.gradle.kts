import net.minecrell.pluginyml.paper.PaperPluginDescription
import org.jooq.meta.jaxb.Logging

plugins {
    `java-library`

    alias(libs.plugins.shadow) // Shades and relocates dependencies, see https://gradleup.com/shadow/
    alias(libs.plugins.run.paper) // Built in test server using runServer and runMojangMappedServer tasks
    alias(libs.plugins.plugin.yml.bukkit) // Automatic plugin.yml generation
    alias(libs.plugins.plugin.yml.paper) // Automatic plugin.yml generation    //alias(libs.plugins.paperweight) // Used to develop internal plugins using Mojang mappings, See https://github.com/PaperMC/paperweight
    alias(libs.plugins.jooq) // Database ORM
    flyway
    projectextensions
    versioner

    eclipse
    idea
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21)) // Configure the java toolchain. This allows gradle to auto-provision JDK 21 on systems that only have JDK 8 installed for example.
    withJavadocJar() // Enable javadoc jar generation
    withSourcesJar() // Enable sources jar generation
}

repositories {
    mavenCentral()

    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://mvn-repo.arim.space/lesser-gpl3/")

    maven("https://maven.athyrium.eu/releases")

    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/") // PlaceholderAPI
    maven("https://repo.codemc.org/repository/maven-public/") {
        content {
            includeGroup("com.github.retrooper") // PacketEvents
        }
    }
    maven("https://jitpack.io/") {
        content {
            includeGroup("com.github.MilkBowl") // VaultAPI
        }
    }
}

dependencies {
    // Core dependencies
    compileOnly(libs.annotations)
    annotationProcessor(libs.annotations)
//    paperweight.paperDevBundle(libs.paper.api.get().version.toString()) // Use instead of the `paper-api` entry if developing plugins using Mojang mappings
    compileOnly(libs.paper.api)
    implementation(libs.morepaperlib)

    // API
    implementation(libs.javasemver) // Required by VersionWatch
    implementation(libs.versionwatch)
    implementation(libs.wordweaver)
    implementation(libs.crate.api)
    implementation(libs.crate.yaml)
    implementation(libs.colorparser) {
        exclude("net.kyori")
    }
    implementation(libs.threadutil.bukkit)
    implementation(libs.commandapi.shade)
    //annotationProcessor(libs.commandapi.annotations) // Uncomment if you want to use command annotations
    implementation(libs.triumph.gui) {
        exclude("net.kyori")
    }

    // Plugin dependencies
    implementation(libs.bstats)
    compileOnly(libs.vault)
    compileOnly(libs.packetevents)
    compileOnly(libs.placeholderapi) {
        exclude("me.clip.placeholderapi.libs", "kyori")
    }

    // Database dependencies - Core
    implementation(libs.hikaricp)
    library(libs.bundles.flyway)
    flywayDriver(libs.h2)
    compileOnly(libs.jakarta) // Compiler bug, see: https://github.com/jOOQ/jOOQ/issues/14865#issuecomment-2077182512
    library(libs.jooq)
    jooqCodegen(libs.h2)

    // Database dependencies - JDBC drivers
    library(libs.bundles.jdbcdrivers)

    // Messaging service clients
    library(libs.bundles.messagingclients)

    // Testing - Core
    testImplementation(libs.annotations)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.bundles.junit)
    testRuntimeOnly(libs.slf4j)
    testImplementation(platform(libs.testcontainers.bom))
    testImplementation(libs.bundles.testcontainers)
    testRuntimeOnly(libs.paper.api)

    // Testing - Database dependencies
    testImplementation(libs.hikaricp)
    testImplementation(libs.bundles.flyway)
    testImplementation(libs.jooq)

    // Testing - JDBC drivers
    testImplementation(libs.bundles.jdbcdrivers)

    // Testing - Messaging service clients
    testImplementation(libs.bundles.messagingclients)
}

tasks {
    // NOTE: Use when developing plugins using Mojang mappings
//    assemble {
//        dependsOn(reobfJar)
//    }

    build {
        dependsOn(shadowJar)
    }

    compileJava {
        options.encoding = Charsets.UTF_8.name() // We want UTF-8 for everything

        // Set the release flag. This configures what version bytecode the compiler will emit, as well as what JDK APIs are usable.
        // See https://openjdk.java.net/jeps/247 for more information.
        options.release.set(21)
        options.compilerArgs.addAll(listOf("-Xlint:all", "-Xlint:-processing", "-Xdiags:verbose"))
    }

    javadoc {
        isFailOnError = false
        val options = options as StandardJavadocDocletOptions
        options.encoding = Charsets.UTF_8.name()
        options.overview = "src/main/javadoc/overview.html"
        options.windowTitle = "${rootProject.name} Javadoc"
        options.tags("apiNote:a:API Note:", "implNote:a:Implementation Note:", "implSpec:a:Implementation Requirements:")
        options.addStringOption("Xdoclint:none", "-quiet")
        options.use()
    }

    processResources {
        filteringCharset = Charsets.UTF_8.name() // We want UTF-8 for everything
    }

    shadowJar {
        archiveBaseName.set(project.name)
        archiveClassifier.set("")

        // Shadow classes
        fun reloc(originPkg: String, targetPkg: String) = relocate(originPkg, "${project.relocationPackage}.${targetPkg}")

        reloc("space.arim.morepaperlib", "morepaperlib")
        reloc("io.github.milkdrinkers.javasemver", "javasemver")
        reloc("io.github.milkdrinkers.versionwatch", "versionwatch")
        reloc("io.github.milkdrinkers.wordweaver", "wordweaver")
        reloc("io.github.milkdrinkers.crate", "crate")
        reloc("io.github.milkdrinkers.colorparser", "colorparser")
        reloc("io.github.milkdrinkers.threadutil", "threadutil")
        reloc("org.snakeyaml", "snakeyaml")
        reloc("org.json", "json")
        reloc("dev.jorel.commandapi", "commandapi")
        reloc("dev.triumphteam.gui", "triumphgui")
        reloc("com.zaxxer.hikari", "hikaricp")
        reloc("org.bstats", "bstats")

        mergeServiceFiles()
    }

    test {
        useJUnitPlatform()
        failFast = false
    }

    runServer {
        // Configure the Minecraft version for our task.
        minecraftVersion(libs.versions.paper.run.get())

        // IntelliJ IDEA debugger setup: https://docs.papermc.io/paper/dev/debugging#using-a-remote-debugger
        jvmArgs("-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005", "-DPaper.IgnoreJavaVersion=true", "-Dcom.mojang.eula.agree=true", "-DIReallyKnowWhatIAmDoingISwear", "-Dpaper.playerconnection.keepalive=6000")
        systemProperty("terminal.jline", false)
        systemProperty("terminal.ansi", true)

        // Automatically install dependencies
        downloadPlugins {
//            modrinth("carbon", "2.1.0-beta.21")
//            github("jpenilla", "MiniMOTD", "v2.0.13", "minimotd-bukkit-2.0.13.jar")
//            hangar("squaremap", "1.2.0")
//            url("https://download.luckperms.net/1515/bukkit/loader/LuckPerms-Bukkit-5.4.102.jar")
            github("MilkBowl", "Vault", "1.7.3", "Vault.jar")
            github("retrooper", "packetevents", "v2.7.0", "packetevents-spigot-2.7.0.jar")
            hangar("PlaceholderAPI", "2.11.6")
            hangar("ViaVersion", "5.4.1")
            hangar("ViaBackwards", "5.4.1")
        }
    }
}

bukkit { // Options: https://github.com/Minecrell/plugin-yml#bukkit
    // Plugin main class (required)
    main = project.entryPointClass

    // Plugin Information
    name = project.name
    prefix = project.name
    version = "${project.version}"
    description = "${project.description}"
    authors = project.authors
    contributors = project.contributors
    apiVersion = libs.versions.paper.api.get().substringBefore("-R")
    foliaSupported = true

    // Misc properties
    load = net.minecrell.pluginyml.bukkit.BukkitPluginDescription.PluginLoadOrder.POSTWORLD // STARTUP or POSTWORLD
    depend = listOf()
    softDepend = listOf("PacketEvents", "Vault", "PlaceholderAPI")
    loadBefore = listOf()
    provides = listOf()
}

paper { // Options: https://github.com/eldoriarpg/plugin-yml/wiki/Paper
    main = project.entryPointClass
    loader = project.entryPointClass + "PluginLoader"
    generateLibrariesJson = true
    load = net.minecrell.pluginyml.bukkit.BukkitPluginDescription.PluginLoadOrder.POSTWORLD

    // Info
    name = project.name
    prefix = project.name
    version = "${project.version}"
    description = "${project.description}"
    authors = project.authors
    contributors = project.contributors
    apiVersion = libs.versions.paper.api.get().substringBefore("-R")
    foliaSupported = false

    // Dependencies
    hasOpenClassloader = true
    bootstrapDependencies {}
    serverDependencies {
        register("PacketEvents") {
            load = PaperPluginDescription.RelativeLoadOrder.BEFORE
            required = false
        }
        register("Vault") {
            load = PaperPluginDescription.RelativeLoadOrder.BEFORE
            required = false
        }
        register("PlaceholderAPI") {
            load = PaperPluginDescription.RelativeLoadOrder.BEFORE
            required = false
        }
    }
    provides = listOf()
}

flyway {
    url = provider {
        "jdbc:h2:${project.layout.buildDirectory.get()}/generated/flyway/db;AUTO_SERVER=TRUE;MODE=MySQL;CASE_INSENSITIVE_IDENTIFIERS=TRUE;IGNORECASE=TRUE"
    }
    user = "sa"
    password = ""
    schemas = listOf("PUBLIC")
    placeholders = mapOf( // Substitute placeholders for flyway
        "tablePrefix" to "",
    )
    validateMigrationNaming = true
    baselineOnMigrate = true
    cleanDisabled = false
    enableRdbmsSpecificMigrations = true
    locations = listOf(
        "filesystem:${project.layout.projectDirectory}/src/main/resources/db/migration/",
        "classpath:${mainPackage.replace(".", "/")}/database/migration/migrations"
    )
}

jooq {
    configuration {
        logging = Logging.ERROR
        jdbc {
            driver = "org.h2.Driver"
            url = flyway.url.get()
            user = flyway.user.get()
            password = flyway.password.get()
        }
        generator {
            database {
                name = "org.jooq.meta.h2.H2Database"
                includes = ".*"
                excludes = "(flyway_schema_history)|(?i:information_schema\\..*)|(?i:system_lobs\\..*)"  // Exclude database specific files
                inputSchema = "PUBLIC"
                schemaVersionProvider = "SELECT :schema_name || '_' || MAX(\"version\") FROM \"flyway_schema_history\"" // Grab version from Flyway
            }
            target {
                packageName = "${mainPackage}.database.schema"
                withClean(true)
            }
        }
    }
}

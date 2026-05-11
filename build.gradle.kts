import versioning.VersioningPlugin

plugins {
    `java-library`
    projectextensions
    versioning
    idea
    eclipse
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21)) // Configure the java toolchain. This allows gradle to auto-provision JDK 21 on systems that only have JDK 8 installed for example.
}

tasks {
    jar {
        enabled = false
    }
}

subprojects {
    apply<JavaLibraryPlugin>()
    apply<ProjectExtensionsPlugin>()
    apply<VersioningPlugin>()

    project.version = rootProject.version
    project.description = rootProject.description

    base.archivesName.set("${rootProject.name}-${project.name}")

    java {
        toolchain.languageVersion.set(JavaLanguageVersion.of(21)) // Configure the java toolchain. This allows gradle to auto-provision JDK 21 on systems that only have JDK 8 installed for example.
        withJavadocJar() // Enable javadoc jar generation
        withSourcesJar() // Enable sources jar generation
    }

    repositories {
        mavenCentral()

        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://mvn-repo.arim.space/lesser-gpl3/")
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

        maven("https://repo.opencollab.dev/maven-snapshots/")
    }

    dependencies {
        compileOnly(rootProject.libs.annotations)

        compileOnly(rootProject.libs.paper.api)
        compileOnly(rootProject.libs.vault)
    }

    tasks {
        compileJava {
            options.release.set(21)
            options.encoding = Charsets.UTF_8.name()
            options.compilerArgs.addAll(arrayListOf("-Xlint:all", "-Xlint:-processing", "-Xdiags:verbose"))
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
            filteringCharset = Charsets.UTF_8.name()
        }

        test {
            useJUnitPlatform()
            failFast = false
        }
    }
}
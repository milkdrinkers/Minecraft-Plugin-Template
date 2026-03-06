plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation(gradleApi())
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("script-runtime"))
    implementation(libs.jgit)
    implementation(libs.flyway)
}

gradlePlugin {
    plugins {
        register("projectextensions") {
            implementationClass = "ProjectExtensionsPlugin"
        }
    }
    plugins {
        register("versioning") {
            implementationClass = "versioning.VersioningPlugin"
        }
    }
    plugins {
        register("flyway") {
            implementationClass = "flyway.FlywayPlugin"
        }
    }
}
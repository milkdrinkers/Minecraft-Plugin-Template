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
        create("projectextensions") {
            id = "projectextensions"
            implementationClass = "ProjectExtensionsPlugin"
        }
    }
    plugins {
        create("versioning") {
            id = "versioning"
            implementationClass = "versioning.VersioningPlugin"
        }
    }
    plugins {
        create("flyway") {
            id = "flyway"
            implementationClass = "flyway.FlywayPlugin"
        }
    }
}
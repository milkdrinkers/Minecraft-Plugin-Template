import org.jooq.meta.jaxb.Logging

plugins {
    alias(libs.plugins.jooq) // Database ORM
    flyway
}

dependencies {
    // Core dependencies
    api(projects.api)
    api(libs.morepaperlib)

    // API
    api(libs.javasemver) // Required by VersionWatch
    api(libs.versionwatch)
    api(libs.wordweaver)
    api(libs.bundles.configurate.core) {
        isTransitive = false
    }
    api(libs.bundles.configurate.yaml) {
        isTransitive = false
    }
    annotationProcessor(libs.configurate.interfaces.ap)
    api(libs.colorparser.common) {
        exclude("net.kyori")
    }
    api(libs.threadutil.common)

    // Database dependencies - Core
    api(libs.hikaricp)
    compileOnly(libs.bundles.flyway)
    flywayDriver(libs.h2)
    compileOnly(libs.jakarta) // Compiler bug, see: https://github.com/jOOQ/jOOQ/issues/14865#issuecomment-2077182512
    compileOnly(libs.jooq)
    jooqCodegen(libs.h2)

    // Database dependencies - JDBC drivers
    compileOnly(libs.bundles.jdbcdrivers)

    // Messaging service clients
    compileOnly(libs.bundles.messagingclients)

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
    rdbmsLocations = listOf("h2", "sqlite", "mysql", "mariadb")
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
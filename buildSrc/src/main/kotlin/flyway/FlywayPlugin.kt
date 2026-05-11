package flyway

import flyway.task.AssimilateMigrationsTask
import flyway.task.FlywayMigrateTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import org.gradle.language.jvm.tasks.ProcessResources
import java.io.File.separator

/**
 * Custom Flyway plugin to provide modern Gradle integration for Flyway with support for jOOQ.
 * @author darksaid98
 */
@Suppress("unused")
class FlywayPlugin : Plugin<Project> {
    companion object {
        private const val PLUGIN_GROUP = "flyway"
        private const val EXTENSION_NAME = "flyway"
        private const val ASSIMILATE_MIGRATIONS_TASK = "assimilateMigrations"
        private const val MIGRATION_PATH = "db/migration"
        private const val TEMP_MIGRATION_PATH = "tmp/flyway/assimilateMigrations/"
        private const val PROCESS_RESOURCES_TASK = "processResources"
        private const val FLYWAY_MIGRATE_TASK = "flywayMigrate"
        private const val JOOQ_CODEGEN_TASK = "jooqCodegen"
        private const val SOURCES_TASK = "sourcesJar"
        private const val COMPILE_JAVA_TASK = "compileJava"
    }

    override fun apply(project: Project) {
        val flywayDriverConfig = project.configurations.create("flywayDriver") {
            description = "Database dependency for Flyway"
            isCanBeConsumed = false
            isCanBeResolved = true
        }

        val extension = project.extensions.create(EXTENSION_NAME, FlywayPluginExtension::class.java)

        val hasProcessResources = project.tasks.names.contains(PROCESS_RESOURCES_TASK)
        val hasJooqCodegen = project.tasks.names.contains(JOOQ_CODEGEN_TASK)

        // Register assimilate migrations task if RDBMS-specific migrations are enabled
        val assimilateMigrationsTask = if (hasProcessResources) {
            registerAssimilateMigrationsTask(project, extension)
        } else {
            project.logger.info("flyway.FlywayPlugin: $PROCESS_RESOURCES_TASK task not found, skipping $ASSIMILATE_MIGRATIONS_TASK task configuration")
            null
        }

        // Register custom Flyway migrate task
        val flywayMigrateTask = registerFlywayMigrateTask(project, extension, flywayDriverConfig, assimilateMigrationsTask)

        // Configure jOOQ codegen
        if (hasJooqCodegen) {
            configureJooqCodegen(project, flywayMigrateTask)
        } else {
            project.logger.info("flyway.FlywayPlugin: $JOOQ_CODEGEN_TASK task not found, skipping jOOQ configuration")
        }

        // Configure processResources if available and RDBMS-specific migrations enabled
        if (hasProcessResources && assimilateMigrationsTask != null) {
            configureProcessResources(project, extension, assimilateMigrationsTask)
        }

        if (hasJooqCodegen) {
            // Required for sources jar generation with jOOQ
            project.tasks.withType<Jar>().configureEach {
                if (name == SOURCES_TASK) {
                    dependsOn(JOOQ_CODEGEN_TASK)
                    duplicatesStrategy = DuplicatesStrategy.INCLUDE
                }
            }

            // Required for javadoc jar generation with jOOQ
            project.tasks.withType<Javadoc>().configureEach {
                exclude("**${separator}database${separator}schema${separator}**")
            }

            // Ensure jOOQ code generation runs before Java compilation
            project.tasks.named<JavaCompile>(COMPILE_JAVA_TASK) {
                dependsOn(JOOQ_CODEGEN_TASK)
            }
        }
    }

    private fun registerAssimilateMigrationsTask(
        project: Project,
        extension: FlywayPluginExtension,
    ): TaskProvider<AssimilateMigrationsTask> {
        return project.tasks.register<AssimilateMigrationsTask>(ASSIMILATE_MIGRATIONS_TASK) {
            group = PLUGIN_GROUP
            description = "Assimilate RDBMS-specific and common migrations into a unified structure that can be used by Flyway."

            config.wireConventionsFrom(extension)
        }
    }

    private fun registerFlywayMigrateTask(
        project: Project,
        extension: FlywayPluginExtension,
        flywayDriverConfig: Configuration,
        assimilateMigrationsTask: TaskProvider<AssimilateMigrationsTask>?,
    ): TaskProvider<FlywayMigrateTask> {
        return project.tasks.register<FlywayMigrateTask>(FLYWAY_MIGRATE_TASK) {
            group = PLUGIN_GROUP
            description = "Run Flyway migrations"

            assimilateMigrationsTask?.let { dependsOn(it) }

            config.wireConventionsFrom(extension)

            // Set JDBC driver classpath
            driverClasspath.from(flywayDriverConfig)

            // Set SQL migrations, filessource or assimilated output if present
            locations.from(
                project.fileTree(project.layout.projectDirectory.dir("src/main/resources/db/migration")) {
                    include("**/*.sql")
                }
            )
            assimilateMigrationsTask?.let { task ->
                locations.from(task.flatMap { it.outputDir })
            }

            // Classpath-based Java/Kotlin migration classes
            val mainPackage = project.findProperty("mainPackage") as? String
            if (mainPackage != null) {
                val classpathDir = "src/main/java/${mainPackage.replace('.', '/')}/database/migration/migrations"
                locationsClasspath.from(
                    project.fileTree(project.layout.projectDirectory.dir(classpathDir)) {
                        include("**/*.java", "**/*.kt")
                    }
                )
            }
        }
    }

    private fun configureProcessResources(
        project: Project,
        extension: FlywayPluginExtension,
        assimilateMigrationsTask: TaskProvider<AssimilateMigrationsTask>
    ) {
        project.tasks.named<ProcessResources>(PROCESS_RESOURCES_TASK) {
            // Only depend on assimilate if RDBMS-specific migrations are enabled
            val enableRdbms = extension.enableRdbmsSpecificMigrations
            dependsOn(assimilateMigrationsTask)

            // Exclude original migration files from being processed directly
            exclude("$MIGRATION_PATH/**")

            val sourceDirProvider = project.layout.buildDirectory.dir(TEMP_MIGRATION_PATH)

            doLast {
                if (!enableRdbms.get()) {
                    return@doLast
                }

                val sourceDir = sourceDirProvider.get().asFile
                val targetDir = destinationDir.resolve(MIGRATION_PATH)

                if (sourceDir.exists()) {
                    sourceDir.copyRecursively(targetDir, overwrite = true)
                    logger.info("flyway.FlywayPlugin: Copied assimilated migrations from $sourceDir to $targetDir")
                } else {
                    logger.warn("flyway.FlywayPlugin: Source directory $sourceDir does not exist, skipping migration copy")
                }
            }
        }
    }

    private fun configureJooqCodegen(
        project: Project,
        flywayMigrateTask: TaskProvider<FlywayMigrateTask>,
    ) {
        project.tasks.named(JOOQ_CODEGEN_TASK) {
            dependsOn(flywayMigrateTask)

            // Track database dir as input
            inputs.files(flywayMigrateTask.flatMap { it.databaseDirectory })

            // Declare generated outputs
            outputs.dirs(
                project.layout.buildDirectory.dir("generated-sources/jooq")
            )
        }
    }
}
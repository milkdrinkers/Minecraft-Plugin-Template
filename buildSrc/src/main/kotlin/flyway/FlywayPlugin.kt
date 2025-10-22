package flyway

import flyway.task.AssimilateMigrationsTask
import flyway.task.FlywayMigrateTask
import flyway.task.InvalidateMigrationsTask
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
import org.gradle.language.jvm.tasks.ProcessResources
import java.io.File
import java.io.File.separator

/**
 * Custom Flyway plugin to provide modern Gradle integration for Flyway with support for jOOQ.
 * @author darksaid98
 */
@Suppress("unused")
abstract class FlywayPlugin : Plugin<Project> {
    companion object {
        private const val PLUGIN_GROUP = "flyway"
        private const val EXTENSION_NAME = "flyway"
        private const val ASSIMILATE_MIGRATIONS_TASK = "assimilateMigrations"
        private const val MIGRATION_PATH = "db/migration"
        private const val TEMP_MIGRATION_PATH = "tmp/assimilateMigrations/"
        private const val INVALIDATE_MIGRATIONS_TASK = "invalidateMigrations"
        private const val PROCESS_RESOURCES_TASK = "processResources"
        private const val FLYWAY_MIGRATE_TASK = "flywayMigrate"
        private const val JOOQ_CODEGEN_TASK = "jooqCodegen"
        private const val SOURCES_TASK = "sourcesJar"
        private const val JAVADOC_TASK = "javadoc"
        private const val COMPILE_JAVA_TASK = "compileJava"
    }

    override fun apply(project: Project) {
        val flywayDriverConfig = project.configurations.create("flywayDriver") {
            description = "Database dependency for Flyway"
            isCanBeConsumed = false
            isCanBeResolved = true
        }

        val extension = project.extensions.create(EXTENSION_NAME, FlywayPluginExtension::class.java, project)

        val hasProcessResources = project.tasks.names.contains(PROCESS_RESOURCES_TASK)
        val hasJooqCodegen = project.tasks.names.contains(JOOQ_CODEGEN_TASK)

        // Register assimilate migrations task if RDBMS-specific migrations are enabled
        val assimilateMigrationsTask = if (hasProcessResources) {
            registerAssimilateMigrationsTask(project, extension)
        } else {
            project.logger.info("flyway.FlywayPlugin: $PROCESS_RESOURCES_TASK task not found, skipping $ASSIMILATE_MIGRATIONS_TASK task configuration")
            null
        }

        // Register invalidate migrations task
        val invalidateMigrationsTask = registerInvalidateMigrationsTask(project)

        // Register custom Flyway migrate task
        val flywayMigrateTask = registerFlywayMigrateTask(project, extension, flywayDriverConfig, assimilateMigrationsTask, invalidateMigrationsTask)

        // Configure jOOQ codegen
        if (hasJooqCodegen) {
            configureJooqCodegen(project, flywayMigrateTask, invalidateMigrationsTask)
        } else {
            project.logger.info("flyway.FlywayPlugin: $JOOQ_CODEGEN_TASK task not found, skipping jOOQ configuration")
        }

        // Configure processResources if available and RDBMS-specific migrations enabled
        if (hasProcessResources && assimilateMigrationsTask != null) {
            configureProcessResources(project, extension, assimilateMigrationsTask)
        }

        project.afterEvaluate {
            val hasSources = project.tasks.names.contains(SOURCES_TASK)
            val hasJavadoc = project.tasks.names.contains(JAVADOC_TASK)
            val hasCompileJava = project.tasks.names.contains(COMPILE_JAVA_TASK)

            // Required for sources jar generation with jOOQ
            if (hasSources && hasJooqCodegen) {
                project.tasks.named<Jar>(SOURCES_TASK) {
                    dependsOn(JOOQ_CODEGEN_TASK)
                    duplicatesStrategy = DuplicatesStrategy.INCLUDE
                }
            }

            // Required for javadoc jar generation with jOOQ
            if (hasJavadoc && hasJooqCodegen) {
                project.tasks.named<Javadoc>(JAVADOC_TASK) {
                    exclude("**${separator}database${separator}schema${separator}**") // Exclude generated jOOQ sources from javadocs
                }
            }

            // Ensure jOOQ code generation runs before Java compilation
            if (hasCompileJava && hasJooqCodegen) {
                project.tasks.named<JavaCompile>(COMPILE_JAVA_TASK) {
                    dependsOn(JOOQ_CODEGEN_TASK)
                }
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

            extension.applyTo(this)

            resourceMigrationDir.set(project.layout.projectDirectory.dir("src/main/resources/db/migration"))
            outputDir.set(project.layout.buildDirectory.dir("tmp/assimilateMigrations"))
        }
    }

    private fun registerInvalidateMigrationsTask(project: Project): TaskProvider<InvalidateMigrationsTask> {
        return project.tasks.register<InvalidateMigrationsTask>(INVALIDATE_MIGRATIONS_TASK) {
            group = PLUGIN_GROUP
            description = "Invalidate Flyway cache based on migration state"

            // Set defaults
            checksumFileName.convention("migration-state.txt")
            outputFile.convention(
                project.layout.buildDirectory.file("tmp/invalidateMigrations/migration-state.txt")
            )

            // Configure file collections
            trackedFilesystemMigrations.from(
                project.fileTree(project.layout.projectDirectory.dir("src/main/resources/db/migration")) {
                    include("**/*.sql")
                }
            )

            val mainPackage = project.findProperty("mainPackage") as? String
            if (mainPackage != null) {
                val classpathDir = "src/main/java/${mainPackage.replace('.', '/')}/database/migration/migrations"
                trackedClasspathMigrations.from(
                    project.fileTree(project.layout.projectDirectory.dir(classpathDir)) {
                        include("**/*.java", "**/*.kt")
                    }
                )
            }

            trackedBuildScripts.from(
                project.rootProject.allprojects.map { it.buildFile }
            )
            trackedBuildScripts.from(
                project.rootProject.files(
                    "settings.gradle.kts",
                    "settings.gradle",
                    "gradle/libs.versions.toml"
                ).filter { it.exists() }
            )
            trackedBuildScripts.from(
                project.files(
                    "settings.gradle.kts",
                    "settings.gradle",
                    "gradle/libs.versions.toml"
                ).filter { it.exists() }
            )

            trackedBuildLogic.from(
                project.rootProject.fileTree("buildSrc/src/main/kotlin") {
                    include("**/*.kt")
                }
            )
            trackedBuildLogic.from(
                project.rootProject.files("build.gradle.kts", "build.gradle").filter { it.exists() }
            )
        }
    }

    private fun registerFlywayMigrateTask(
        project: Project,
        extension: FlywayPluginExtension,
        flywayDriverConfig: Configuration,
        assimilateMigrationsTask: TaskProvider<AssimilateMigrationsTask>?,
        invalidateMigrationsTask: TaskProvider<InvalidateMigrationsTask>
    ): TaskProvider<FlywayMigrateTask> {
        return project.tasks.register<FlywayMigrateTask>(FLYWAY_MIGRATE_TASK) {
            group = PLUGIN_GROUP
            description = "Run Flyway migrations (configuration cache compatible)"

            // Dependencies
            dependsOn(invalidateMigrationsTask)
            if (assimilateMigrationsTask != null && extension.enableRdbmsSpecificMigrations.getOrElse(false)) {
                dependsOn(assimilateMigrationsTask)
            }

            extension.applyTo(this)

            // Set JDBC driver classpath
            driverClasspath.from(flywayDriverConfig)

            // Connect migration state file
            migrationStateFile.set(invalidateMigrationsTask.flatMap { it.outputFile })
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
            if (enableRdbms.getOrElse(false)) {
                dependsOn(assimilateMigrationsTask)
            }

            // Exclude original migration files from being processed directly
            exclude("$MIGRATION_PATH/**")

            // Use Provider API to avoid capturing project instance
            val buildDir = project.layout.buildDirectory
            val sourceDirProvider = buildDir.dir(TEMP_MIGRATION_PATH)

            doLast {
                if (!enableRdbms.getOrElse(false)) {
                    return@doLast
                }

                val sourceDir = sourceDirProvider.get().asFile
                val targetDir = File(destinationDir, MIGRATION_PATH)

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
        invalidateMigrationsTask: TaskProvider<InvalidateMigrationsTask>
    ) {
        project.tasks.named(JOOQ_CODEGEN_TASK) {
            dependsOn(flywayMigrateTask)

            // Track migration-state.txt to invalidate cache
            inputs.files(invalidateMigrationsTask.flatMap { it.outputFile })

            // Track database dir as input
            inputs.files(flywayMigrateTask.flatMap { it.databaseDirectory })

            // Declare generated outputs
            outputs.dirs(
                project.layout.buildDirectory.dir("generated-sources/jooq")
            )
        }
    }
}
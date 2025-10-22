package flyway.task

import org.flywaydb.core.Flyway
import org.flywaydb.core.api.output.MigrateResult
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import java.io.File.separator
import java.net.URLClassLoader

/**
 * A task executing Flyway migrate.
 */
abstract class FlywayMigrateTask : DefaultTask() {
    @get:Input
    abstract val url: Property<String>

    @get:Input
    @get:Optional
    abstract val driver: Property<String>

    @get:Input
    abstract val user: Property<String>

    @get:Input
    abstract val password: Property<String>

    @get:Input
    abstract val schemas: ListProperty<String>

    @get:Input
    @get:Optional
    abstract val defaultSchema: Property<String>

    @get:Input
    abstract val locations: ListProperty<String>

    @get:Input
    abstract val placeholders: MapProperty<String, String>

    @get:Input
    abstract val placeholderPrefix: Property<String>

    @get:Input
    abstract val placeholderSuffix: Property<String>

    @get:Input
    abstract val placeholderReplacement: Property<Boolean>

    @get:Input
    abstract val validateMigrationNaming: Property<Boolean>

    @get:Input
    abstract val validateOnMigrate: Property<Boolean>

    @get:Input
    abstract val baselineOnMigrate: Property<Boolean>

    @get:Input
    abstract val baselineVersion: Property<String>

    @get:Input
    abstract val baselineDescription: Property<String>

    @get:Input
    abstract val outOfOrder: Property<Boolean>

    @get:Input
    abstract val mixed: Property<Boolean>

    @get:Input
    abstract val groupMigrations: Property<Boolean>

    @get:Input
    abstract val cleanOnValidationError: Property<Boolean>

    @get:Input
    abstract val cleanDisabled: Property<Boolean>

    @get:Input
    abstract val table: Property<String>

    @get:Input
    @get:Optional
    abstract val tablespace: Property<String>

    @get:Input
    abstract val encoding: Property<String>

    @get:Input
    abstract val sqlMigrationPrefix: Property<String>

    @get:Input
    abstract val repeatableSqlMigrationPrefix: Property<String>

    @get:Input
    abstract val sqlMigrationSeparator: Property<String>

    @get:Input
    abstract val sqlMigrationSuffixes: ListProperty<String>

    @get:Input
    abstract val callbacks: ListProperty<String>

    @get:Input
    @get:Optional
    abstract val target: Property<String>

    @get:InputFiles
    @get:Classpath
    abstract val driverClasspath: ConfigurableFileCollection

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val migrationStateFile: RegularFileProperty

    @get:Input
    abstract val enableRdbmsSpecificMigrations: Property<Boolean>

    @get:Input
    @get:Optional
    abstract val buildDirectoryPath: Property<String>

    @get:OutputDirectory
    abstract val databaseDirectory: RegularFileProperty

    init {
        buildDirectoryPath.convention(project.layout.buildDirectory.map { it.asFile.absolutePath })

        databaseDirectory.convention(
            project.layout.buildDirectory.file("generated/flyway/")
        )
    }

    @TaskAction
    fun migrate() {
        logger.info("Running Flyway migration...")

        // Compute locations used by plugin
        val computedLocations = if (enableRdbmsSpecificMigrations.get()) {
            val jdbcPrefix = extractJdbcPrefix(url.get())
            logger.debug("Attempting to use RDBMS specific migrations")
            if (jdbcPrefix != null) {
                logger.debug("Extracted JDBC URL: {}", url.get())
                locations.get().filter { !it.startsWith("filesystem:", true) } +
                    "filesystem:${buildDirectoryPath.get()}${separator}tmp${separator}assimilateMigrations${separator}$jdbcPrefix"
            } else {
                locations.get()
            }
        } else {
            locations.get()
        }

        logger.debug("Locations: {}", computedLocations)

        // Configure flyway
        val driverUrls = driverClasspath.files.map { it.toURI().toURL() }.toTypedArray()
        val driverClassLoader = URLClassLoader(driverUrls, this::class.java.classLoader)

        val config = Flyway.configure(driverClassLoader)
            .dataSource(url.get(), user.get(), password.get())
            .schemas(*schemas.get().toTypedArray())
            .locations(*computedLocations.toTypedArray())
            .placeholders(placeholders.get())
            .placeholderPrefix(placeholderPrefix.get())
            .placeholderSuffix(placeholderSuffix.get())
            .placeholderReplacement(placeholderReplacement.get())
            .validateMigrationNaming(validateMigrationNaming.get())
            .validateOnMigrate(validateOnMigrate.get())
            .baselineOnMigrate(baselineOnMigrate.get())
            .baselineVersion(baselineVersion.get())
            .baselineDescription(baselineDescription.get())
            .outOfOrder(outOfOrder.get())
            .mixed(mixed.get())
            .group(groupMigrations.get())
            .cleanOnValidationError(cleanOnValidationError.get())
            .cleanDisabled(cleanDisabled.get())
            .table(table.get())
            .encoding(encoding.get())
            .sqlMigrationPrefix(sqlMigrationPrefix.get())
            .repeatableSqlMigrationPrefix(repeatableSqlMigrationPrefix.get())
            .sqlMigrationSeparator(sqlMigrationSeparator.get())
            .sqlMigrationSuffixes(*sqlMigrationSuffixes.get().toTypedArray())

        // Optional properties
        driver.orNull?.let { config.driver(it) }
        defaultSchema.orNull?.let { config.defaultSchema(it) }
        tablespace.orNull?.let { config.tablespace(it) }
        target.orNull?.let { config.target(it) }

        if (callbacks.get().isNotEmpty()) {
            config.callbacks(*callbacks.get().toTypedArray())
        }

        val flyway = config.load()
        val result: MigrateResult = flyway.migrate()

        logger.info("Flyway migration completed: ${result.migrationsExecuted} migrations executed")
        if (result.warnings.isNotEmpty())
            logger.warn("Flyway warnings: ${result.warnings}")
    }

    /**
     * Extracts the RDBMS JDBC prefix from a JDBC URL. (IE: 'jdbc:mysql' returns 'mysql')
     */
    fun extractJdbcPrefix(jdbcUrl: String): String? {
        val regex = Regex("^jdbc:([^:]+):")
        return regex.find(jdbcUrl)?.groupValues?.get(1)
    }
}
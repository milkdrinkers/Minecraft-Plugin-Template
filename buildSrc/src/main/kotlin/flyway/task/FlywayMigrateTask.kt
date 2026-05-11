package flyway.task

import flyway.FlywayConfig
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.output.MigrateResult
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import java.io.File.separator
import java.net.URLClassLoader

/**
 * A task executing Flyway migrate.
 * @author darksaid98
 */
@CacheableTask
abstract class FlywayMigrateTask : DefaultTask() {
    @get:Nested
    abstract val config: FlywayConfig

    @get:InputFiles
    @get:Classpath
    abstract val driverClasspath: ConfigurableFileCollection

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val locations: ConfigurableFileCollection

    @get:InputFiles
    @get:Classpath
    abstract val locationsClasspath: ConfigurableFileCollection

    @get:Input
    @get:Optional
    abstract val buildDirectoryPath: Property<String>

    @get:OutputDirectory
    abstract val databaseDirectory: DirectoryProperty

    init {
        buildDirectoryPath.convention(project.layout.buildDirectory.map { it.asFile.absolutePath })
        databaseDirectory.convention(project.layout.buildDirectory.dir("generated/flyway/"))
    }

    @TaskAction
    fun migrate() {
        logger.info("Running Flyway migration...")

        // Compute locations used by plugin
        val computedLocations = if (config.enableRdbmsSpecificMigrations.get()) {
            val jdbcPrefix = extractJdbcPrefix(config.url.get())
            logger.debug("Attempting to use RDBMS specific migrations")
            if (jdbcPrefix != null) {
                logger.debug("Extracted JDBC URL: {}", config.url.get())
                config.locations.get().filter { !it.startsWith("filesystem:", true) } +
                    "filesystem:${buildDirectoryPath.get()}${separator}tmp${separator}flyway${separator}assimilateMigrations${separator}$jdbcPrefix"
            } else {
                config.locations.get()
            }
        } else {
            config.locations.get()
        }

        logger.debug("Locations: {}", computedLocations)

        val driverUrls = driverClasspath.files.map { it.toURI().toURL() }.toTypedArray()
        val driverClassLoader = URLClassLoader(driverUrls, this::class.java.classLoader)

        val flywayConfig = Flyway.configure(driverClassLoader)
            .dataSource(config.url.get(), config.user.get(), config.password.get())
            .schemas(*config.schemas.get().toTypedArray())
            .locations(*computedLocations.toTypedArray())
            .placeholders(config.placeholders.get())
            .placeholderPrefix(config.placeholderPrefix.get())
            .placeholderSuffix(config.placeholderSuffix.get())
            .placeholderReplacement(config.placeholderReplacement.get())
            .validateMigrationNaming(config.validateMigrationNaming.get())
            .validateOnMigrate(config.validateOnMigrate.get())
            .baselineOnMigrate(config.baselineOnMigrate.get())
            .baselineVersion(config.baselineVersion.get())
            .baselineDescription(config.baselineDescription.get())
            .outOfOrder(config.outOfOrder.get())
            .mixed(config.mixed.get())
            .group(config.groupMigrations.get())
            .cleanDisabled(config.cleanDisabled.get())
            .table(config.table.get())
            .encoding(config.encoding.get())
            .sqlMigrationPrefix(config.sqlMigrationPrefix.get())
            .repeatableSqlMigrationPrefix(config.repeatableSqlMigrationPrefix.get())
            .sqlMigrationSeparator(config.sqlMigrationSeparator.get())
            .sqlMigrationSuffixes(*config.sqlMigrationSuffixes.get().toTypedArray())

        config.driver.orNull?.let { flywayConfig.driver(it) }
        config.defaultSchema.orNull?.let { flywayConfig.defaultSchema(it) }
        config.tablespace.orNull?.let { flywayConfig.tablespace(it) }
        config.target.orNull?.let { flywayConfig.target(it) }

        if (config.callbacks.get().isNotEmpty()) {
            flywayConfig.callbacks(*config.callbacks.get().toTypedArray())
        }

        val flyway = flywayConfig.load()
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

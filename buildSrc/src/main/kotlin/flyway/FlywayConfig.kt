package flyway

import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional

/**
 * The base configuration for Flyway tasks and plugin extension.
 * @author darksaid98
 */
abstract class FlywayConfig {
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

    @get:Input
    abstract val enableRdbmsSpecificMigrations: Property<Boolean>

    @get:Input
    abstract val rdbmsLocations: ListProperty<String>

    internal fun applyConventions() {
        schemas.convention(listOf("PUBLIC"))
        placeholders.convention(emptyMap())
        callbacks.convention(emptyList())
        locations.convention(listOf())
        placeholderPrefix.convention($$"${")
        placeholderSuffix.convention("}")
        placeholderReplacement.convention(true)
        validateMigrationNaming.convention(true)
        validateOnMigrate.convention(true)
        baselineOnMigrate.convention(false)
        baselineVersion.convention("1")
        baselineDescription.convention("<< Flyway Baseline >>")
        outOfOrder.convention(false)
        mixed.convention(false)
        groupMigrations.convention(false)
        cleanDisabled.convention(true)
        table.convention("flyway_schema_history")
        encoding.convention("UTF-8")
        sqlMigrationPrefix.convention("V")
        repeatableSqlMigrationPrefix.convention("R")
        sqlMigrationSeparator.convention("__")
        sqlMigrationSuffixes.convention(listOf(".sql"))
        enableRdbmsSpecificMigrations.convention(false)
        rdbmsLocations.convention(listOf())
    }

    internal fun wireConventionsFrom(source: FlywayConfig) {
        url.convention(source.url)
        driver.convention(source.driver)
        user.convention(source.user)
        password.convention(source.password)
        schemas.convention(source.schemas)
        defaultSchema.convention(source.defaultSchema)
        locations.convention(source.locations)
        placeholders.convention(source.placeholders)
        placeholderPrefix.convention(source.placeholderPrefix)
        placeholderSuffix.convention(source.placeholderSuffix)
        placeholderReplacement.convention(source.placeholderReplacement)
        validateMigrationNaming.convention(source.validateMigrationNaming)
        validateOnMigrate.convention(source.validateOnMigrate)
        baselineOnMigrate.convention(source.baselineOnMigrate)
        baselineVersion.convention(source.baselineVersion)
        baselineDescription.convention(source.baselineDescription)
        outOfOrder.convention(source.outOfOrder)
        mixed.convention(source.mixed)
        groupMigrations.convention(source.groupMigrations)
        cleanDisabled.convention(source.cleanDisabled)
        table.convention(source.table)
        tablespace.convention(source.tablespace)
        encoding.convention(source.encoding)
        sqlMigrationPrefix.convention(source.sqlMigrationPrefix)
        repeatableSqlMigrationPrefix.convention(source.repeatableSqlMigrationPrefix)
        sqlMigrationSeparator.convention(source.sqlMigrationSeparator)
        sqlMigrationSuffixes.convention(source.sqlMigrationSuffixes)
        callbacks.convention(source.callbacks)
        target.convention(source.target)
        enableRdbmsSpecificMigrations.convention(source.enableRdbmsSpecificMigrations)
        rdbmsLocations.convention(source.rdbmsLocations)
    }
}

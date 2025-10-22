package flyway

import flyway.task.AssimilateMigrationsTask
import flyway.task.FlywayMigrateTask
import org.gradle.api.Project
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.listProperty
import org.gradle.kotlin.dsl.property

/**
 * The configuring object for the Flyway plugin.
 */
abstract class FlywayPluginExtension(project: Project) {
    val url: Property<String> = project.objects.property()
    val driver: Property<String> = project.objects.property()
    val user: Property<String> = project.objects.property()
    val password: Property<String> = project.objects.property()
    val schemas: ListProperty<String> = project.objects.listProperty()
    val defaultSchema: Property<String> = project.objects.property()
    val locations: ListProperty<String> = project.objects.listProperty()
    val placeholders: MapProperty<String, String> = project.objects.mapProperty(String::class.java, String::class.java)
    val placeholderPrefix: Property<String> = project.objects.property<String>().convention($$"${")
    val placeholderSuffix: Property<String> = project.objects.property<String>().convention("}")
    val placeholderReplacement: Property<Boolean> = project.objects.property<Boolean>().convention(true)
    val validateMigrationNaming: Property<Boolean> = project.objects.property<Boolean>().convention(true)
    val validateOnMigrate: Property<Boolean> = project.objects.property<Boolean>().convention(true)
    val baselineOnMigrate: Property<Boolean> = project.objects.property<Boolean>().convention(false)
    val baselineVersion: Property<String> = project.objects.property<String>().convention("1")
    val baselineDescription: Property<String> = project.objects.property<String>().convention("<< Flyway Baseline >>")
    val outOfOrder: Property<Boolean> = project.objects.property<Boolean>().convention(false)
    val mixed: Property<Boolean> = project.objects.property<Boolean>().convention(false)
    val groupMigrations: Property<Boolean> = project.objects.property<Boolean>().convention(false)
    val cleanOnValidationError: Property<Boolean> = project.objects.property<Boolean>().convention(false)
    val cleanDisabled: Property<Boolean> = project.objects.property<Boolean>().convention(true)
    val table: Property<String> = project.objects.property<String>().convention("flyway_schema_history")
    val tablespace: Property<String> = project.objects.property()
    val encoding: Property<String> = project.objects.property<String>().convention("UTF-8")
    val sqlMigrationPrefix: Property<String> = project.objects.property<String>().convention("V")
    val repeatableSqlMigrationPrefix: Property<String> = project.objects.property<String>().convention("R")
    val sqlMigrationSeparator: Property<String> = project.objects.property<String>().convention("__")
    val sqlMigrationSuffixes: ListProperty<String> = project.objects.listProperty<String>().convention(listOf(".sql"))
    val callbacks: ListProperty<String> = project.objects.listProperty()
    val target: Property<String> = project.objects.property()
    val enableRdbmsSpecificMigrations: Property<Boolean> = project.objects.property<Boolean>().convention(false)

    init {
        schemas.convention(listOf("PUBLIC"))
        placeholders.convention(emptyMap())
        callbacks.convention(emptyList())
        locations.convention(project.provider { listOf() })
    }

    /**
     * Apply global configuration to task.
     */
    internal fun applyTo(task: AssimilateMigrationsTask) {
        task.sqlMigrationSuffixes.set(sqlMigrationSuffixes)
    }

    /**
     * Apply global configuration to task.
     */
    internal fun applyTo(task: FlywayMigrateTask) {
        task.url.set(url)
        task.driver.set(driver)
        task.user.set(user)
        task.password.set(password)
        task.schemas.set(schemas)
        task.defaultSchema.set(defaultSchema)
        task.locations.set(locations)
        task.placeholders.set(placeholders)
        task.placeholderPrefix.set(placeholderPrefix)
        task.placeholderSuffix.set(placeholderSuffix)
        task.placeholderReplacement.set(placeholderReplacement)
        task.validateMigrationNaming.set(validateMigrationNaming)
        task.validateOnMigrate.set(validateOnMigrate)
        task.baselineOnMigrate.set(baselineOnMigrate)
        task.baselineVersion.set(baselineVersion)
        task.baselineDescription.set(baselineDescription)
        task.outOfOrder.set(outOfOrder)
        task.mixed.set(mixed)
        task.groupMigrations.set(groupMigrations)
        task.cleanOnValidationError.set(cleanOnValidationError)
        task.cleanDisabled.set(cleanDisabled)
        task.table.set(table)
        task.tablespace.set(tablespace)
        task.encoding.set(encoding)
        task.sqlMigrationPrefix.set(sqlMigrationPrefix)
        task.repeatableSqlMigrationPrefix.set(repeatableSqlMigrationPrefix)
        task.sqlMigrationSeparator.set(sqlMigrationSeparator)
        task.sqlMigrationSuffixes.set(sqlMigrationSuffixes)
        task.enableRdbmsSpecificMigrations.set(enableRdbmsSpecificMigrations)
        task.callbacks.set(callbacks)
        task.target.set(target)
    }
}
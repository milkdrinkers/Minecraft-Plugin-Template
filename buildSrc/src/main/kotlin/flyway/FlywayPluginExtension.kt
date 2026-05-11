package flyway

/**
 * The configuring object for the Flyway plugin.
 * @author darksaid98
 */
abstract class FlywayPluginExtension : FlywayConfig() {
    init {
        applyConventions()
    }
}

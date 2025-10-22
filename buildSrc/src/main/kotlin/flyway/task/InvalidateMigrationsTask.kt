package flyway.task

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import java.io.File
import java.security.MessageDigest

/**
 * A task that generates a migration cache file based on the inputs to our FlyWay task.
 */
abstract class InvalidateMigrationsTask : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val trackedFilesystemMigrations: ConfigurableFileCollection

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val trackedClasspathMigrations: ConfigurableFileCollection

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val trackedBuildScripts: ConfigurableFileCollection

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val trackedBuildLogic: ConfigurableFileCollection

    @get:Input
    abstract val checksumFileName: Property<String>

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @TaskAction
    fun generateMigrationState() {
        val outputFile = outputFile.get().asFile
        outputFile.parentFile.mkdirs()

        val migrationState = buildString {
            appendFileStates("filesystem", trackedFilesystemMigrations.files)
            appendFileStates("classpath", trackedClasspathMigrations.files)
            appendFileStates("buildscript", trackedBuildScripts.files)
            appendFileStates("buildlogic", trackedBuildLogic.files)
        }

        outputFile.writeText(migrationState)
        logger.info("Generated migration state file: ${outputFile.absolutePath}")
    }

    private fun StringBuilder.appendFileStates(category: String, files: Set<File>) {
        files.forEach { file ->
            try {
                val hash = calculateFileHash(file)
                appendLine("$category:${file.canonicalPath}:$hash")
            } catch (e: Exception) {
                logger.warn("Failed to calculate hash for ${file.absolutePath}: ${e.message}")
                appendLine("$category:${file.canonicalPath}:lastmod-${file.lastModified()}")
            }
        }
    }

    private fun calculateFileHash(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = file.readBytes()
        val hashBytes = digest.digest(bytes)
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
}
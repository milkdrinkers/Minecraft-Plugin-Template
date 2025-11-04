package versioning

import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.property
import java.io.File

/**
 * The configuration object for the Versioning plugin.
 */
abstract class VersioningPluginExtension(
    private val project: Project
) {
    companion object {
        internal const val EXTENSION_NAME = "versioning"
    }

    val applyProject: Property<Boolean> = project.objects.property<Boolean>().convention(true)
    val applySubProjects: Property<Boolean> = project.objects.property<Boolean>().convention(true)
    val useGit: Property<Boolean> = project.objects.property<Boolean>().convention(true)
    val gitDirectory: Property<File> = project.objects.property<File>()
    val autoIncrementSnapshot: Property<Boolean> = project.objects.property<Boolean>().convention(true)

    init {
        gitDirectory.convention(project.provider {
            project.rootDir.resolve(".git")
        })
    }

    /**
     * Get the computed version as a Provider for build cache compatibility.
     * This provider captures all inputs that affect the version.
     */
    internal fun getVersionProvider(
        gitVersionProvider: Provider<GitVersionValueSource.GitInfo>,
        originalVersion: String
    ): Provider<String> {
        return project.provider {
            val versionOverride = project.findProperty("altVer")?.toString()

            if (versionOverride != null) {
                stripLeadingV(versionOverride)
            } else {
                createSnapshotVersion(originalVersion, gitVersionProvider.orNull)
            }
        }
    }

    /**
     * Generates a pre-release version.
     */
    private fun createSnapshotVersion(
        originalVersion: String,
        gitVersion: GitVersionValueSource.GitInfo?
    ): String {
        val cleanVersion = stripLeadingV(originalVersion)
        val preReleaseName = project.findProperty("snapshot")?.toString() ?: "SNAPSHOT"

        // Increment minor version to differentiate snapshot from existing releases
        val version = incrementVersion(cleanVersion)

        // Snapshot version without git info
        if (!useGit.get() || gitVersion == null)
            return "$version-$preReleaseName"

        return "$version-$preReleaseName+${gitVersion.lastCommitHash.uppercase()}"
    }

    /**
     * Strips the character 'v' from the start of the string if present
     */
    private fun stripLeadingV(version: String): String {
        return if (version.firstOrNull()?.equals('v', ignoreCase = true) == true) {
            version.substring(1)
        } else {
            version
        }
    }

    /**
     * Bumps the patch version of a semantic versioning string by replacing the third number with +1.
     */
    private fun incrementVersion(version: String): String {
        return if (autoIncrementSnapshot.get()) {
            version.replace(Regex("""^(\d+\.\d+\.)(\d+)""")) { matchResult ->
                val prefix = matchResult.groupValues[1]
                val patch = matchResult.groupValues[2].toInt() + 1
                "$prefix$patch"
            }
        } else {
            version
        }
    }
}
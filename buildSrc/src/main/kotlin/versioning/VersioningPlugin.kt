package versioning

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.of

/**
 * A plugin to help in dealing with project versioning.
 * @author darksaid98
 */
@Suppress("unused")
abstract class VersioningPlugin : Plugin<Project> {
    override fun apply(project: Project): Unit = with(project) {
        val extension = extensions.create<VersioningPluginExtension>(
            VersioningPluginExtension.EXTENSION_NAME,
            project
        )
        val gitVersionProvider = project.providers.of(GitVersionValueSource::class) {
            parameters {
                gitDir.set(extension.gitDirectory)
            }
        }
        val originalVersion = project.version.toString()

        afterEvaluate {
            applyVersioning(project, extension, gitVersionProvider, originalVersion)
        }
    }

    /**
     * Applies custom versioning to project and subprojects as configured.
     */
    private fun applyVersioning(
        project: Project,
        extension: VersioningPluginExtension,
        gitVersionProvider: Provider<GitVersionValueSource.GitInfo>,
        originalVersion: String
    ) {
        if (extension.applyProject.get()) {
            overrideProjectVersion(project, extension, gitVersionProvider, originalVersion)
        }

        if (extension.applySubProjects.get()) {
            applyToSubProjects(project, extension, gitVersionProvider)
        }
    }

    private fun applyToSubProjects(
        project: Project,
        extension: VersioningPluginExtension,
        gitVersionProvider: Provider<GitVersionValueSource.GitInfo>
    ) {
        project.subprojects.forEach { subproject ->
            val subOriginalVersion = subproject.version.toString()
            overrideProjectVersion(subproject, extension, gitVersionProvider, subOriginalVersion)
            applyToSubProjects(subproject, extension, gitVersionProvider)
        }
    }

    private fun overrideProjectVersion(
        project: Project,
        extension: VersioningPluginExtension,
        gitVersionProvider: Provider<GitVersionValueSource.GitInfo>,
        originalVersion: String
    ) {
        val versionProvider = extension.getVersionProvider(gitVersionProvider, originalVersion)
        val newVersion = versionProvider.get()

        project.version = newVersion
        project.logger.debug("Set project version for '{}' to: '{}'", project.name, project.version)
    }
}
package versioning

import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.gradle.api.logging.Logger
import org.gradle.api.provider.Property
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import java.io.File
import javax.inject.Inject

/**
 * Get the latest commit hash from a local git repository.
 */
abstract class GitVersionValueSource : ValueSource<GitVersionValueSource.GitInfo, GitVersionValueSource.Params> {
    interface Params : ValueSourceParameters {
        val gitDir: Property<File>
    }

    @get:Inject
    abstract val logger: Logger

    data class GitInfo(
        val lastCommitHash: String
    )

    override fun obtain(): GitInfo? {
        return runCatching {
            val gitDir = parameters.gitDir.orNull
            if (gitDir == null || !gitDir.exists()) {
                throw IllegalStateException("Git repository not found")
            }

            val repository = FileRepositoryBuilder()
                .setGitDir(gitDir)
                .readEnvironment()
                .findGitDir()
                .build()

            return repository.use { repo ->
                val head = repo.resolve("HEAD") ?: throw IllegalStateException("Could not resolve HEAD")
                val commitHash = head.abbreviate(7).name()
                GitInfo(commitHash)
            }
        }.onFailure { e ->
            logger.debug("Error getting git information: ${e.message}")
        }.getOrNull()
    }
}
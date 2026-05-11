package versioning

import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.logging.Logging
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters

/**
 * Get the latest commit hash from a local git repository.
 * @author darksaid98
 */
abstract class GitVersionValueSource : ValueSource<GitVersionValueSource.GitInfo, GitVersionValueSource.Params> {
    interface Params : ValueSourceParameters {
        val gitDir: DirectoryProperty
    }

    data class GitInfo(
        val lastCommitHash: String
    )

    override fun obtain(): GitInfo? {
        val logger = Logging.getLogger(GitVersionValueSource::class.java)
        return runCatching {
            val gitDir = parameters.gitDir.orNull?.asFile
            if (gitDir == null || !gitDir.exists()) {
                throw IllegalStateException("Git directory not found at: $gitDir")
            }

            val repository = FileRepositoryBuilder()
                .setGitDir(gitDir)
                .readEnvironment()
                .findGitDir()
                .build()

            repository.use { repo ->
                val head = repo.resolve("HEAD") ?: throw IllegalStateException("Could not resolve HEAD")
                val commitHash = head.abbreviate(7).name()
                GitInfo(commitHash)
            }
        }.onFailure { e ->
            logger.debug("Error getting git information: ${e.message}", e)
        }.getOrNull()
    }
}
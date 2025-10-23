import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.internal.extensions.core.extra

/**
 * Utility plugin to add common project extensions methods.
 */
@Suppress("unused")
abstract class ProjectExtensionsPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.extra["mainPackage"] = getMainPackage(project)
        project.extra["entryPointClass"] = getEntryPointClass(project)
        project.extra["relocationPackage"] = getRelocationPackage(project)
        project.extra["authors"] = getAuthors(project)
        project.extra["contributors"] = getContributors(project)
    }

    private fun getMainPackage(project: Project): String {
        return "${project.rootProject.group}.${project.rootProject.name.lowercase()}"
    }

    private fun getEntryPointClass(project: Project): String {
        return "${getMainPackage(project)}.${project.rootProject.name}"
    }

    private fun getRelocationPackage(project: Project): String {
        return "${getMainPackage(project)}.lib"
    }

    private fun getAuthors(project: Project): List<String> {
        val authors = project.rootProject.properties["authors"] ?: return emptyList()

        if (authors !is String)
            return emptyList()

        if (authors.isBlank())
            return emptyList()

        return authors.split(",", " ").filter { s -> !s.isBlank() }
    }

    private fun getContributors(project: Project): List<String> {
        val contributors = project.rootProject.properties["contributors"] ?: return emptyList()

        if (contributors !is String)
            return emptyList()

        if (contributors.isBlank())
            return emptyList()

        return contributors.split(",", " ").filter { s -> !s.isBlank() }
    }
}

val Project.mainPackage: String
    get() = extra["mainPackage"] as String

val Project.entryPointClass: String
    get() = extra["entryPointClass"] as String

val Project.relocationPackage: String
    get() = extra["relocationPackage"] as String

@Suppress("UNCHECKED_CAST")
val Project.authors: List<String>
    get() = extra["authors"] as List<String>

@Suppress("UNCHECKED_CAST")
val Project.contributors: List<String>
    get() = extra["contributors"] as List<String>
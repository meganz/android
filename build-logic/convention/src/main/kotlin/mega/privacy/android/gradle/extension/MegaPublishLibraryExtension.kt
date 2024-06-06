package mega.privacy.android.gradle.extension

/**
 * extension for publishing library
 *
 * @property groupId group Id
 * @property artifactId artifact Id
 * @property version version
 * @property libPath local relative path of the aar file
 * @property sourcePath local relative path of the source jar
 * @property repoKey repository key in Maven
 * @property properties properties that can be attached to the artifact
 * @property dependentTasks tasks that need to be executed before publishing
 */
open class MegaPublishLibraryExtension(
    var groupId: String = "",
    var artifactId: String = "",
    var version: String = "",
    var libPath: String = "",
    var sourcePath: String = "",
    var repoKey: String = "",
    var properties: Map<String, String> = emptyMap(),
    var dependentTasks: List<String> = emptyList(),
) {
    override fun toString(): String =
        "MegaPublishLibraryExtension(groupId='$groupId', artifactId='$artifactId', version='$version', libPath='$libPath', sourcePath='$sourcePath')"
}
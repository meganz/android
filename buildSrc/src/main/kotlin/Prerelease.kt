package src.main.kotlin

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import java.io.File


/**
 * PreReleaseTask
 */
abstract class PreReleaseTask : DefaultTask() {

    /**
     * Version
     */
    @get:Input
    @set:Option(
        option = "rv",
        description = "The version of the upcoming release as a string. eg. \"9.3\""
    )
    abstract var version: String

    /**
     * doPreRelease
     */
    @TaskAction
    fun doPreRelease() {
        println("The version is $version")
        val branchName = createBranch()
        updateVersion()
        updateStrings()
        createMergeRequest(branchName)
    }

    private fun createBranch(): String {
        val branchName = "task/pre-release/v$version"
        project.exec {
            commandLine("git", "checkout", "-b", branchName)
        }
        return branchName
    }

    private fun updateVersion() {
        val buildGradleFile = File("build.gradle.kts")
        buildGradleFile.writeText(
            buildGradleFile.readText().replaceFirst(
                Regex("extra\\[\"appVersion\"\\] = \".*\""),
                "extra[\"appVersion\"] = \"$version\""
            )
        )

        // Add changes
        project.exec {
            workingDir = project.rootDir
            commandLine("git", "add", "build.gradle.kts")
        }

        project.exec {
            commandLine("git", "commit", "-m", "Update version")
        }
    }

    private fun updateStrings() {

        project.exec {
            workingDir = File(project.rootDir, "transifex")
            commandLine("git", "pull")
        }

        project.exec {
            workingDir = File(project.rootDir, "transifex")
            commandLine("./androidTransifex.py", "-m", "export")
        }

        val filteredFiles = project.fileTree(project.rootDir) {
            include("**/strings_change_log.xml")
        }

        project.delete(filteredFiles)

        project.exec {
            workingDir = project.rootDir
            commandLine("git", "add", "-A")
        }

        project.exec {
            commandLine("git", "commit", "-m", "Update strings")
        }
    }

    private fun createMergeRequest(branch: String) {
        project.exec {
            workingDir = project.rootDir
            commandLine(
                "git",
                "push",
                "--set-upstream",
                "origin",
                branch,
                "-o",
                "merge_request.create",
                "-o",
                "merge_request.title=Pre-release - v$version",
            )
        }
    }

}
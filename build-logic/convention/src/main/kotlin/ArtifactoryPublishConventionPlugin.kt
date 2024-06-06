import mega.privacy.android.gradle.extension.MegaPublishLibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.create
import org.jfrog.gradle.plugin.artifactory.dsl.ArtifactoryPluginConvention


/**
 * Plugin to publish library to Maven on artifactory
 */
class ArtifactoryPublishConventionPlugin : Plugin<Project> {
    private lateinit var publishExtension: MegaPublishLibraryExtension

    /**
     * Apply this plugin to the given target project.
     *
     * @param target target project
     */
    override fun apply(target: Project) {

        with(target) {
            with(pluginManager) {
                apply("com.jfrog.artifactory")
                apply("maven-publish")
            }

            publishExtension =
                extensions.create("megaPublish", MegaPublishLibraryExtension::class.java)

            afterEvaluate {
                println("PublishLibConventionPlugin:extensions = $publishExtension")
                extensions.configure<PublishingExtension> {
                    publications {
                        create<MavenPublication>(getPublicationName(target)) {
                            groupId = publishExtension.groupId
                            artifactId = publishExtension.artifactId
                            version = publishExtension.version
                            artifact(publishExtension.libPath)
                            artifact(publishExtension.sourcePath) {
                                classifier = "sources"
                                extension = "jar"
                            }
                        }
                    }
                }

                configure<ArtifactoryPluginConvention> {
                    clientConfig.isIncludeEnvVars = true

                    System.getenv("ARTIFACTORY_BASE_URL")
                        ?.let { serverUrl ->
                            setContextUrl("$serverUrl/artifactory/mega-gradle"); serverUrl
                        }
                        ?: failWithError("ARTIFACTORY_BASE_URL can not be null or empty in the environment")

                    publish {
                        repository {
                            setRepoKey(publishExtension.repoKey)

                            System.getenv("ARTIFACTORY_USER")
                                ?.let { user -> setUsername(user) }
                            System.getenv("ARTIFACTORY_ACCESS_TOKEN")
                                ?.let { token -> setPassword(token) }
                        }
                        defaults {
                            setPublishArtifacts(true)
                            publications("aar")
                            setPublishPom(true)
                            setProperties(publishExtension.properties)
                            setPublishIvy(false)
                        }
                    }
                }
                tasks.getByName("artifactoryPublish") {
                    dependsOn(publishExtension.dependentTasks)
                }
            }
        }
    }

    private fun getPublicationName(project: Project): String {
        with(project.pluginManager) {
            when {
                hasPlugin("com.android.library") -> return "aar"
                hasPlugin("com.android.application") -> return "aar"
                else -> return "jar"
            }
        }
    }

    private fun failWithError(errorMsg: String): Unit = error(errorMsg)
}

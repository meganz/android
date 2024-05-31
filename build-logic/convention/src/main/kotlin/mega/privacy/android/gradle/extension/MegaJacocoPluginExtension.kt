package mega.privacy.android.gradle.extension

/**
 * Extension for Mega Jacoco plugin.
 *
 * Then in the module's build.gradle.kts file, you can configure the JaCoCo plugin like this:
 * ```
 * `mega-jacoco` {
 *    excludedFiles = setOf("**YourPattern1.class", "**YourPattern2.class")
 *    includedFiles = setOf("**YourPattern3.class", "**YourPattern4.class")
 *    }
 */
open class MegaJacocoPluginExtension(

    /**
     * Set the list of files if more files or packages need to be excluded,
     * on top of the default excluded files and packages
     */
    var excludedFiles: Set<String> = emptySet(),

    /**
     * Set the list of files if you want to remove some files from the exclusion list
     */
    var includedFiles: Set<String> = emptySet(),

    /**
     * Default excluded files and packages. Each plugin type(Android app, Android library
     * or JVM library) defines its own default excluded files.
     */
    var defaultExcludedFiles: Set<String> = emptySet(),
)
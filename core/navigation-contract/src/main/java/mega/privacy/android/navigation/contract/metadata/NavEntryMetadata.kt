package mega.privacy.android.navigation.contract.metadata

import androidx.navigation3.runtime.NavEntry

/**
 * Builds navigation entry metadata in a declarative way.
 *
 * Any module can contribute to metadata by adding extension functions on [NavEntryMetadataScope]
 * or by calling [NavEntryMetadataScope.set] for custom key-value pairs.
 *
 * Example:
 * ```
 * metadata = buildMetadata {
 *     withAnalytics(HomeScreenEvent)       // from analytics-tracker
 *     set("custom_key", customValue)       // custom properties
 * }
 * ```
 *
 * @param block Configuration block for the metadata
 * @return Map suitable for use as [NavEntry] metadata
 */
fun buildMetadata(block: NavEntryMetadataScope.() -> Unit): Map<String, Any> =
    NavEntryMetadataScope().apply(block).build()

/**
 * Scope for building navigation entry metadata declaratively.
 *
 * Use [set] to add custom key-value pairs. Other modules (e.g. analytics-tracker) provide
 * extension functions such as `withAnalytics` to register their properties.
 */
class NavEntryMetadataScope {

    private val metadata = mutableMapOf<String, Any>()

    /**
     * Sets a key-value pair in the metadata.
     * Use for custom metadata or from module-specific extension functions.
     */
    fun set(key: String, value: Any) {
        metadata[key] = value
    }

    internal fun build(): Map<String, Any> = metadata
}

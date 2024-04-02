package mega.privacy.android.domain.entity.search

/**
 * Enum class to represent the types filter options
 *
 * @param title The title of the filter option
 */
// TODO Create mapper like SearchFilterMapper to get strings with context
enum class TypeFilterOption(val title: String) {
    /**
     * Images filter option
     */
    Images("Images"),

    /**
     * Documents filter option
     */
    Documents("Documents"),

    /**
     * Audio filter option
     */
    Audio("Audio"),

    /**
     * Videos filter option
     */
    Video("Video"),
}

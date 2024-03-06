package mega.privacy.android.app.presentation.search.model

/**
 * Enum class to represent the types filter options
 *
 * @param title The title of the filter option
 */
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

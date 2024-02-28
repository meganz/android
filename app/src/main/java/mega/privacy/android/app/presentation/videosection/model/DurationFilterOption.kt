package mega.privacy.android.app.presentation.videosection.model

/**
 * Enum class to represent the duration filter option.
 *
 * @param title The title of the filter option.
 */
enum class DurationFilterOption(val title: String) {
    /**
     * Less than 4 minutes filter option.
     */
    LessThan4("Less than 4 minutes"),

    /**
     * Between 4 and 20 minutes filter option.
     */
    Between4And20("Between 4 and 20 minutes"),


    /**
     * More than 20 minutes filter option.
     */
    MoreThan20("More than 20 minutes"),
}
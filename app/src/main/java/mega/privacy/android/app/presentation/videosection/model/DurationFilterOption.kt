package mega.privacy.android.app.presentation.videosection.model

/**
 * Enum class to represent the duration filter option.
 *
 * @param title The title of the filter option.
 */
enum class DurationFilterOption(val title: String) {

    /**
     * All durations filter option.
     */
    AllDurations("All durations"),

    /**
     * Less than 10 seconds filter option.
     */
    LessThan10Seconds("Less than 10 seconds"),

    /**
     * Between 10 and 60 seconds filter option.
     */
    Between10And60Seconds("Between 10 and 60 seconds"),

    /**
     * Between 1 and 4 minutes filter option.
     */
    Between1And4("Between 1 and 4 minutes"),

    /**
     * Between 4 and 20 minutes filter option.
     */
    Between4And20("Between 4 and 20 minutes"),


    /**
     * More than 20 minutes filter option.
     */
    MoreThan20("More than 20 minutes"),
}
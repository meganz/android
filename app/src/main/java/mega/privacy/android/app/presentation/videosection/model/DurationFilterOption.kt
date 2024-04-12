package mega.privacy.android.app.presentation.videosection.model

import mega.privacy.android.shared.resources.R

/**
 * Enum class to represent the duration filter option.
 *
 * @param titleResId The title resource id of the filter option.
 */
enum class DurationFilterOption(val titleResId: Int) {

    /**
     * All durations filter option.
     */
    AllDurations(R.string.video_section_videos_duration_option_all_duration),

    /**
     * Less than 10 seconds filter option.
     */
    LessThan10Seconds(R.string.video_section_videos_duration_option_less_than_10_seconds),

    /**
     * Between 10 and 60 seconds filter option.
     */
    Between10And60Seconds(R.string.video_section_videos_duration_option_between_10_and_60_seconds),

    /**
     * Between 1 and 4 minutes filter option.
     */
    Between1And4(R.string.video_section_videos_duration_option_between_1_and_4_minutes),

    /**
     * Between 4 and 20 minutes filter option.
     */
    Between4And20(R.string.video_section_videos_duration_option_between_4_and_20_minutes),


    /**
     * More than 20 minutes filter option.
     */
    MoreThan20(R.string.video_section_videos_duration_option_more_than_20_minutes),
}
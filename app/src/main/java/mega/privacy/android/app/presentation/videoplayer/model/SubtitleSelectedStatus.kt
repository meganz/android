package mega.privacy.android.app.presentation.videoplayer.model

/**
 * Enum class representing the status of selected subtitles.
 */
enum class SubtitleSelectedStatus(val id: Int) {

    /**
     * Subtitle off status
     */
    Off(SUBTITLE_SELECTED_STATE_OFF),

    /**
     * Select matched item status
     */
    SelectMatchedItem(SUBTITLE_SELECTED_STATE_MATCHED_ITEM),

    /**
     * Add subtitle item status
     */
    AddSubtitleItem(SUBTITLE_SELECTED_STATE_ADD_SUBTITLE_ITEM),
}

/**
 * The state for the off is selected
 */
const val SUBTITLE_SELECTED_STATE_OFF = 900

/**
 * The state for the matched item is selected
 */
const val SUBTITLE_SELECTED_STATE_MATCHED_ITEM = 901

/**
 * The state for the add subtitle item is selected
 */
const val SUBTITLE_SELECTED_STATE_ADD_SUBTITLE_ITEM = 902
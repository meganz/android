package mega.privacy.android.data.mapper.mediaplayer

import mega.privacy.android.domain.entity.mediaplayer.RepeatToggleMode
import javax.inject.Inject

/**
 * The mapper class for converting the data entity to [RepeatToggleMode]
 */
class RepeatToggleModeMapper @Inject constructor() {
    /**
     * Invocation function
     *
     * @param repeatMode Int value of repeat mode
     * @return RepeatToggleMode
     */
    operator fun invoke(repeatMode: Int?) =
        when (repeatMode) {
            null -> RepeatToggleMode.REPEAT_NONE
            RepeatToggleMode.REPEAT_NONE.ordinal -> RepeatToggleMode.REPEAT_NONE
            RepeatToggleMode.REPEAT_ONE.ordinal -> RepeatToggleMode.REPEAT_ONE
            else -> RepeatToggleMode.REPEAT_ALL
        }
}
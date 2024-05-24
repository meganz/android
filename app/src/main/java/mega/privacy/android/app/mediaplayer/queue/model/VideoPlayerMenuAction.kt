package mega.privacy.android.app.mediaplayer.queue.model

import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.app.R
import mega.privacy.android.shared.original.core.ui.model.MenuAction
import mega.privacy.android.shared.original.core.ui.model.MenuActionString
import mega.privacy.android.shared.original.core.ui.model.MenuActionWithoutIcon

/**
 * Video player menu action
 */
sealed interface VideoPlayerMenuAction : MenuAction {

    /**
     * Video queue select action
     */
    object VideoQueueSelectAction : MenuActionWithoutIcon(
        descriptionRes = R.string.general_select,
        testTag = TEST_TAG_VIDEO_QUEUE_SELECT_ACTION
    ), VideoPlayerMenuAction {
        override val orderInCategory = 140
    }

    /**
     * Video queue select action
     */
    object VideoQueueRemoveAction : MenuActionString(
        iconRes = iconPackR.drawable.ic_x_medium_regular_outline,
        descriptionRes = R.string.general_remove,
        testTag = TEST_TAG_VIDEO_QUEUE_REMOVE_ACTION
    ), VideoPlayerMenuAction {
        override val orderInCategory = 145
    }

    companion object {
        /**
         * Test tag for video queue select action
         */
        const val TEST_TAG_VIDEO_QUEUE_SELECT_ACTION = "video_player:action_video_queue_select"

        /**
         * Test tag for video queue remove action
         */
        const val TEST_TAG_VIDEO_QUEUE_REMOVE_ACTION = "video_player:action_video_queue_remove"
    }
}
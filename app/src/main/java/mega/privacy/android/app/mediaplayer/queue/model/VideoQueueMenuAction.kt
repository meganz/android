package mega.privacy.android.app.mediaplayer.queue.model

import mega.privacy.android.app.R
import mega.privacy.android.icon.pack.IconPack
import mega.android.core.ui.model.menu.MenuAction
import mega.android.core.ui.model.menu.MenuActionString
import mega.android.core.ui.model.menu.MenuActionWithoutIcon

/**
 * Video player menu action
 */
sealed interface VideoQueueMenuAction : MenuAction {

    /**
     * Video queue select action
     */
    object VideoQueueSelectAction : MenuActionWithoutIcon(
        descriptionRes = R.string.general_select,
        testTag = TEST_TAG_VIDEO_QUEUE_SELECT_ACTION
    ), VideoQueueMenuAction {
        override val orderInCategory = 140
    }

    /**
     * Video queue select action
     */
    object VideoQueueRemoveAction : MenuActionString(
        icon = IconPack.Medium.Thin.Outline.X,
        descriptionRes = R.string.general_remove,
        testTag = TEST_TAG_VIDEO_QUEUE_REMOVE_ACTION
    ), VideoQueueMenuAction {
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
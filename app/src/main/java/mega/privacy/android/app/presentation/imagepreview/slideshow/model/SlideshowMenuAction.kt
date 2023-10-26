package mega.privacy.android.app.presentation.imagepreview.slideshow.model

import mega.privacy.android.app.R
import mega.privacy.android.core.ui.model.MenuAction
import mega.privacy.android.core.ui.model.MenuActionString

/**
 * Slideshow Menu action
 */
sealed interface SlideshowMenuAction : MenuAction {

    object SettingOptionsMenuAction : MenuActionString(
        iconRes = R.drawable.ic_options,
        descriptionRes = R.string.slideshow_settings_page_title,
        testTag = TEST_TAG_SETTING_OPTIONS_ACTION,
    ), SlideshowMenuAction {
        override val orderInCategory = 100
    }

    companion object {
        const val TEST_TAG_SETTING_OPTIONS_ACTION = "slideshow_view:action_setting_options"
    }
}
package mega.privacy.android.app.presentation.imagepreview.slideshow.model

import mega.privacy.android.app.R
import mega.privacy.android.icon.pack.IconPack
import mega.android.core.ui.model.menu.MenuAction
import mega.android.core.ui.model.menu.MenuActionString

/**
 * Slideshow Menu action
 */
sealed interface SlideshowMenuAction : MenuAction {

    object SettingOptionsMenuAction : MenuActionString(
        icon = IconPack.Medium.Thin.Outline.GearSix,
        descriptionRes = R.string.slideshow_settings_page_title,
        testTag = TEST_TAG_SETTING_OPTIONS_ACTION,
    ), SlideshowMenuAction {
        override val orderInCategory = 100
    }

    object SettingTutorialMenuAction : MenuActionString(
        icon = IconPack.Medium.Thin.Outline.ShieldInfo,
        descriptionRes = mega.privacy.android.shared.resources.R.string.slideshow_tutorial_title,
        testTag = TEST_TAG_SETTING_TUTORIAL_INFO_ACTION,
    ), SlideshowMenuAction {
        override val orderInCategory = 99
    }

    companion object {
        const val TEST_TAG_SETTING_OPTIONS_ACTION = "slideshow_view:action_setting_options"
        const val TEST_TAG_SETTING_TUTORIAL_INFO_ACTION = "slideshow_view:action_tutorial_options"
    }
}
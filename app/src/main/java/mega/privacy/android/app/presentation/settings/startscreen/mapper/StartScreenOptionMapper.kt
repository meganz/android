package mega.privacy.android.app.presentation.settings.startscreen.mapper

import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.settings.startscreen.model.StartScreenOption
import mega.privacy.android.domain.entity.preference.StartScreen
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.shared.resources.R as sharedR

/**
 * Map [mega.privacy.android.domain.entity.preference.StartScreen] to [mega.privacy.android.app.presentation.settings.startscreen.model.StartScreenOption]
 */
typealias StartScreenOptionMapper = (@JvmSuppressWildcards StartScreen) -> @JvmSuppressWildcards StartScreenOption<StartScreen>?

/**
 * Map [StartScreen] to [StartScreenOption]
 *
 * @param screen
 */
internal fun mapStartScreenOption(screen: StartScreen) = when (screen) {
    StartScreen.CloudDrive -> {
        StartScreenOption(
            startScreen = screen,
            title = R.string.section_cloud_drive,
            icon = IconPack.Medium.Thin.Outline.Folder,
        )
    }

    StartScreen.Photos -> {
        StartScreenOption(
            startScreen = screen,
            title = R.string.settings_start_screen_photos_option,
            icon = IconPack.Medium.Thin.Outline.Camera,
        )
    }

    StartScreen.Home -> {
        StartScreenOption(
            startScreen = screen,
            title = sharedR.string.general_section_home,
            icon = IconPack.Medium.Thin.Outline.Mega,
        )
    }

    StartScreen.Chat -> {
        StartScreenOption(
            startScreen = screen,
            title = R.string.section_chat,
            icon = IconPack.Medium.Thin.Outline.MessageChatCircle,
        )
    }

    StartScreen.SharedItems -> {
        StartScreenOption(
            startScreen = screen,
            title = R.string.title_shared_items,
            icon = IconPack.Medium.Thin.Outline.FolderUsers,
        )
    }
}
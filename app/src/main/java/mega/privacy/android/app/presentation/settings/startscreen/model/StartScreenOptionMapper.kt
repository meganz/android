package mega.privacy.android.app.presentation.settings.startscreen.model

import mega.privacy.android.app.R
import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.domain.entity.preference.StartScreen

/**
 * Map [StartScreen] to [StartScreenOption]
 */
typealias StartScreenOptionMapper = (@JvmSuppressWildcards StartScreen) -> @JvmSuppressWildcards StartScreenOption?

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
            icon = iconPackR.drawable.ic_folder_medium_regular_outline,
        )
    }

    StartScreen.Photos -> {
        StartScreenOption(
            startScreen = screen,
            title = R.string.settings_start_screen_photos_option,
            icon = iconPackR.drawable.ic_camera_medium_regular_outline,
        )
    }

    StartScreen.Home -> {
        StartScreenOption(
            startScreen = screen,
            title = R.string.home_section,
            icon = iconPackR.drawable.ic_mega_medium_regular_outline,
        )
    }

    StartScreen.Chat -> {
        StartScreenOption(
            startScreen = screen,
            title = R.string.section_chat,
            icon = iconPackR.drawable.ic_message_chat_circle_medium_regular_outline,
        )
    }

    StartScreen.SharedItems -> {
        StartScreenOption(
            startScreen = screen,
            title = R.string.title_shared_items,
            icon = iconPackR.drawable.ic_folder_users_medium_regular_outline,
        )
    }
}
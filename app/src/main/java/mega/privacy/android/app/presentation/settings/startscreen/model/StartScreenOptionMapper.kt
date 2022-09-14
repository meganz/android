package mega.privacy.android.app.presentation.settings.startscreen.model

import mega.privacy.android.app.R
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
            icon = R.drawable.ic_files_home,
        )
    }
    StartScreen.Photos -> {
        StartScreenOption(
            startScreen = screen,
            title = R.string.settings_start_screen_photos_option,
            icon = R.drawable.ic_camera_uploads,
        )
    }
    StartScreen.Home -> {
        StartScreenOption(
            startScreen = screen,
            title = R.string.home_section,
            icon = R.drawable.ic_homepage,
        )
    }
    StartScreen.Chat -> {
        StartScreenOption(
            startScreen = screen,
            title = R.string.section_chat,
            icon = R.drawable.ic_chat,
        )
    }
    StartScreen.SharedItems -> {
        StartScreenOption(
            startScreen = screen,
            title = R.string.title_shared_items,
            icon = R.drawable.ic_shared,
        )
    }
}
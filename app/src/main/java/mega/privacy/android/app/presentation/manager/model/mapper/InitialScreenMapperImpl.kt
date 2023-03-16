package mega.privacy.android.app.presentation.manager.model.mapper

import mega.privacy.android.app.main.DrawerItem
import mega.privacy.android.domain.entity.preference.StartScreen

/**
 * Initial screen mapper impl
 */
class InitialScreenMapperImpl : InitialScreenMapper {
    override fun invoke(startScreen: StartScreen) = when (startScreen) {
        StartScreen.CloudDrive -> DrawerItem.CLOUD_DRIVE
        StartScreen.Photos -> DrawerItem.PHOTOS
        StartScreen.Home -> DrawerItem.HOMEPAGE
        StartScreen.Chat -> DrawerItem.CHAT
        StartScreen.SharedItems -> DrawerItem.SHARED_ITEMS
    }
}
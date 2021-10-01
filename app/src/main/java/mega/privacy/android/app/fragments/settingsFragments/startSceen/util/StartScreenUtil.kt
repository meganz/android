package mega.privacy.android.app.fragments.settingsFragments.startSceen.util

import android.content.Context
import mega.privacy.android.app.constants.SettingsConstants
import mega.privacy.android.app.lollipop.ManagerActivityLollipop.*

/**
 * Class storing util methods for the start screen setting.
 */
object StartScreenUtil {

    const val CLOUD_DRIVE = 0
    const val CAMERA_UPLOADS = 1
    const val HOME = 2
    const val CHAT = 3
    const val SHARED_ITEMS = 4

    /**
     * Gets the start DrawerItem depending on the preferred start screen chosen.
     *
     * @param context   Current context.
     * @return The start DrawerItem.
     */
    @JvmStatic
    fun getStartDrawerItem(context: Context): DrawerItem =
        when (context.getSharedPreferences(
            SettingsConstants.USER_INTERFACE_PREFERENCES,
            Context.MODE_PRIVATE
        )
            .getInt(
                SettingsConstants.PREFERRED_START_SCREEN,
                SettingsConstants.DEFAULT_PREFERRED_START_SCREEN
            )) {
            CLOUD_DRIVE -> DrawerItem.CLOUD_DRIVE
            CAMERA_UPLOADS -> DrawerItem.CAMERA_UPLOADS
            CHAT -> DrawerItem.CHAT
            SHARED_ITEMS -> DrawerItem.SHARED_ITEMS
            else -> DrawerItem.HOMEPAGE
        }


    /**
     * Gets the start bottom navigation item depending on the preferred start screen chosen.
     *
     * @param context   Current context.
     * @return The start bottom navigation item.
     */
    @JvmStatic
    fun getStartBottomNavigationItem(context: Context): Int =
        when (context.getSharedPreferences(
            SettingsConstants.USER_INTERFACE_PREFERENCES,
            Context.MODE_PRIVATE
        )
            .getInt(
                SettingsConstants.PREFERRED_START_SCREEN,
                SettingsConstants.DEFAULT_PREFERRED_START_SCREEN
            )) {
            CLOUD_DRIVE -> CLOUD_DRIVE_BNV
            CAMERA_UPLOADS -> CAMERA_UPLOADS_BNV
            CHAT -> CHAT_BNV
            SHARED_ITEMS -> SHARED_BNV
            else -> HOMEPAGE_BNV
        }

    /**
     * Checks if should close the app because the current DrawerItem is the preferred start screen.
     *
     * @param startItem     Preferred start screen item.
     * @param drawerItem    Current drawerItem.
     * @return True if should close the app, false otherwise.
     */
    @JvmStatic
    fun shouldCloseApp(startItem: Int, drawerItem: DrawerItem): Boolean =
        (drawerItem == DrawerItem.CLOUD_DRIVE && startItem == CLOUD_DRIVE_BNV)
                || (drawerItem == DrawerItem.CAMERA_UPLOADS && startItem == CAMERA_UPLOADS_BNV)
                || (drawerItem == DrawerItem.CHAT && startItem == CHAT_BNV)
                || (drawerItem == DrawerItem.SHARED_ITEMS && startItem == SHARED_BNV)
                || (drawerItem == DrawerItem.HOMEPAGE && startItem == HOMEPAGE_BNV)
}
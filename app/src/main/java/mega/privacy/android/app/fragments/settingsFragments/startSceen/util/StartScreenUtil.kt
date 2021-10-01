package mega.privacy.android.app.fragments.settingsFragments.startSceen.util

import android.content.Context
import mega.privacy.android.app.lollipop.ManagerActivityLollipop.*
import mega.privacy.android.app.utils.Constants.INVALID_VALUE

/**
 * Class storing util methods for the start screen setting.
 */
object StartScreenUtil {

    const val CLOUD_DRIVE = 0
    const val CAMERA_UPLOADS = 1
    const val HOME = 2
    const val CHAT = 3
    const val SHARED_ITEMS = 4


    const val USER_INTERFACE_PREFERENCES = "USER_INTERFACE_PREFERENCES"
    const val PREFERRED_START_SCREEN = "PREFERRED_START_SCREEN"
    const val START_SCREEN_LOGIN_TIMESTAMP = "START_SCREEN_LOGIN_TIMESTAMP"
    private const val TIME_TO_SHOW_START_SCREEN_DIALOG: Long = 604800 //1 week in seconds
    const val ALERT_ABOUT_START_SCREEN = "ALERT_ABOUT_START_SCREEN"
    const val HIDE_RECENT_ACTIVITY = "HIDE_RECENT_ACTIVITY"

    /**
     * Gets the start DrawerItem depending on the preferred start screen chosen.
     *
     * @param context   Current Context.
     * @return The start DrawerItem.
     */
    @JvmStatic
    fun getStartDrawerItem(context: Context): DrawerItem =
        when (context.getSharedPreferences(USER_INTERFACE_PREFERENCES, Context.MODE_PRIVATE)
            .getInt(PREFERRED_START_SCREEN, HOME)) {
            CLOUD_DRIVE -> DrawerItem.CLOUD_DRIVE
            CAMERA_UPLOADS -> DrawerItem.CAMERA_UPLOADS
            CHAT -> DrawerItem.CHAT
            SHARED_ITEMS -> DrawerItem.SHARED_ITEMS
            else -> DrawerItem.HOMEPAGE
        }


    /**
     * Gets the start bottom navigation item depending on the preferred start screen chosen.
     *
     * @param context   Current Context.
     * @return The start bottom navigation item.
     */
    @JvmStatic
    fun getStartBottomNavigationItem(context: Context): Int =
        when (context.getSharedPreferences(USER_INTERFACE_PREFERENCES, Context.MODE_PRIVATE)
            .getInt(PREFERRED_START_SCREEN, HOME)) {
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


    /**
     * Checks if should show the dialog to inform about choose the preferred start screen.
     *
     * @param context   Current Context.
     * @return True if should show the dialog, false otherwise.
     */
    fun shouldShowStartScreenDialog(context: Context): Boolean {
        val preferences =
            context.getSharedPreferences(USER_INTERFACE_PREFERENCES, Context.MODE_PRIVATE)

        val alert = preferences.getBoolean(ALERT_ABOUT_START_SCREEN, false)
        val preferredScreen = preferences.getInt(PREFERRED_START_SCREEN, HOME)

        if (alert || preferredScreen != HOME) {
            return false
        }

        val timeStamp = preferences.getLong(START_SCREEN_LOGIN_TIMESTAMP, INVALID_VALUE.toLong())

        return timeStamp == INVALID_VALUE.toLong()
                || System.currentTimeMillis() - timeStamp >= TIME_TO_SHOW_START_SCREEN_DIALOG
    }

    /**
     * Sets is not needed to alert anymore about start screen.
     *
     * @param context   Current Context.
     */
    fun notAlertAnymoreAboutStartScreen(context: Context) {
        context.getSharedPreferences(USER_INTERFACE_PREFERENCES, Context.MODE_PRIVATE)
            .edit().putBoolean(ALERT_ABOUT_START_SCREEN, true).apply()
    }

    /**
     * Starts the time to show the choose start screen dialog.
     *
     * @param context   Current context.
     */
    @JvmStatic
    fun setStartScreenTimeStamp(context: Context) {
        context.getSharedPreferences(USER_INTERFACE_PREFERENCES, MODE_PRIVATE)
            .edit().putLong(START_SCREEN_LOGIN_TIMESTAMP, System.currentTimeMillis()).apply()
    }
}
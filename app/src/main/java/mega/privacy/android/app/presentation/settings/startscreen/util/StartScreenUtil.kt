package mega.privacy.android.app.presentation.settings.startscreen.util

import android.content.Context
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import mega.privacy.android.app.di.settings.startscreen.getMonitorStartScreenPreference
import mega.privacy.android.app.main.DrawerItem
import mega.privacy.android.app.main.ManagerActivity.MODE_PRIVATE
import mega.privacy.android.app.utils.Constants.INVALID_VALUE
import mega.privacy.android.app.utils.SharedPreferenceConstants.DO_NOT_ALERT_ABOUT_START_SCREEN
import mega.privacy.android.app.utils.SharedPreferenceConstants.START_SCREEN_LOGIN_TIMESTAMP
import mega.privacy.android.app.utils.SharedPreferenceConstants.USER_INTERFACE_PREFERENCES
import mega.privacy.android.domain.entity.preference.StartScreen

/**
 * Class storing util methods for the start screen setting.
 */
object StartScreenUtil {

    @JvmField
    val CLOUD_DRIVE_BNV = StartScreen.CloudDrive.id

    @JvmField
    val PHOTOS_BNV = StartScreen.Photos.id

    @JvmField
    val HOME_BNV = StartScreen.Home.id

    @JvmField
    val CHAT_BNV = StartScreen.Chat.id

    @JvmField
    val SHARED_ITEMS_BNV = StartScreen.SharedItems.id

    @JvmField
    val NO_BNV = 5

    private const val TIME_TO_SHOW_START_SCREEN_DIALOG = 604800000 //1 week in milliseconds

    /**
     * Gets the start DrawerItem depending on the preferred start screen chosen.
     *
     * @return The start DrawerItem.
     */
    @JvmStatic
    fun getStartDrawerItem(): DrawerItem =
        runBlocking {
            when (getMonitorStartScreenPreference()().map { it.id }.first()) {
                CLOUD_DRIVE_BNV -> DrawerItem.CLOUD_DRIVE
                PHOTOS_BNV -> DrawerItem.PHOTOS
                CHAT_BNV -> DrawerItem.CHAT
                SHARED_ITEMS_BNV -> DrawerItem.SHARED_ITEMS
                else -> DrawerItem.HOMEPAGE
            }
        }


    /**
     * Gets the start bottom navigation item depending on the preferred start screen chosen.
     *
     * @return The start bottom navigation item.
     */
    @JvmStatic
    fun getStartBottomNavigationItem(): Int = getStartScreenId()

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
                || (drawerItem == DrawerItem.PHOTOS && startItem == PHOTOS_BNV)
                || (drawerItem == DrawerItem.CHAT && startItem == CHAT_BNV)
                || (drawerItem == DrawerItem.SHARED_ITEMS && startItem == SHARED_ITEMS_BNV)
                || (drawerItem == DrawerItem.HOMEPAGE && startItem == HOME_BNV)


    /**
     * Checks if should show the dialog to inform about choose the preferred start screen.
     *
     * @param context   Current Context.
     * @return True if should show the dialog, false otherwise.
     */
    fun shouldShowStartScreenDialog(context: Context): Boolean {
        val preferences =
            context.getSharedPreferences(USER_INTERFACE_PREFERENCES, Context.MODE_PRIVATE)

        val doNotAlert = preferences.getBoolean(DO_NOT_ALERT_ABOUT_START_SCREEN, false)
        val preferredScreen = getStartScreenId()

        if (doNotAlert || preferredScreen != HOME_BNV) {
            return false
        }

        val timeStamp = preferences.getLong(START_SCREEN_LOGIN_TIMESTAMP, INVALID_VALUE.toLong())

        return timeStamp == INVALID_VALUE.toLong()
                || System.currentTimeMillis().minus(timeStamp) >= TIME_TO_SHOW_START_SCREEN_DIALOG
    }

    private fun getStartScreenId() =
        runBlocking {
            getMonitorStartScreenPreference()()
                .map { it.id }.first()
        }


    /**
     * Sets is not needed to alert anymore about start screen.
     *
     * @param context   Current Context.
     */
    fun notAlertAnymoreAboutStartScreen(context: Context) {
        context.getSharedPreferences(USER_INTERFACE_PREFERENCES, Context.MODE_PRIVATE)
            .edit().putBoolean(DO_NOT_ALERT_ABOUT_START_SCREEN, true).apply()
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
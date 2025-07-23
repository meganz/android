package mega.privacy.android.app.presentation.settings.startscreen.util

import android.content.Context
import android.content.Context.MODE_PRIVATE
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import mega.privacy.android.app.main.DrawerItem
import mega.privacy.android.app.utils.Constants.INVALID_VALUE
import mega.privacy.android.app.utils.SharedPreferenceConstants.DO_NOT_ALERT_ABOUT_START_SCREEN
import mega.privacy.android.app.utils.SharedPreferenceConstants.START_SCREEN_LOGIN_TIMESTAMP
import mega.privacy.android.app.utils.SharedPreferenceConstants.USER_INTERFACE_PREFERENCES
import mega.privacy.android.domain.entity.preference.StartScreen
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.usecase.MonitorStartScreenPreference
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Class storing util methods for the start screen setting.
 * Temporary solution until the new navigation is implemented.
 */
@Singleton
class StartScreenUtil @Inject constructor(
    @ApplicationContext private val context: Context,
    @ApplicationScope coroutineScope: CoroutineScope,
    monitorStartScreenPreference: MonitorStartScreenPreference,
) {
    /**
     * Flow that monitors the start screen preference.
     */
    val startScreen = monitorStartScreenPreference()
        .stateIn(coroutineScope, started = SharingStarted.Eagerly, initialValue = StartScreen.Home)

    /**
     * Gets the start DrawerItem depending on the preferred start screen chosen.
     *
     * @return The start DrawerItem.
     */
    fun getStartDrawerItem(): DrawerItem =
        when (startScreen.value.id) {
            CLOUD_DRIVE_BNV -> DrawerItem.CLOUD_DRIVE
            PHOTOS_BNV -> DrawerItem.PHOTOS
            CHAT_BNV -> DrawerItem.CHAT
            SHARED_ITEMS_BNV -> DrawerItem.SHARED_ITEMS
            else -> DrawerItem.HOMEPAGE
        }


    /**
     * Gets the start bottom navigation item depending on the preferred start screen chosen.
     *
     * @return The start bottom navigation item.
     */
    fun getStartBottomNavigationItem(): Int = getStartScreenId()

    /**
     * Checks if should close the app because the current DrawerItem is the preferred start screen.
     *
     * @param startItem     Preferred start screen item.
     * @param drawerItem    Current drawerItem.
     * @return True if should close the app, false otherwise.
     */
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
    fun shouldShowStartScreenDialog(): Boolean {
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

    private fun getStartScreenId() = startScreen.value.id

    /**
     * Sets is not needed to alert anymore about start screen.
     *
     * @param context   Current Context.
     */
    fun notAlertAnymoreAboutStartScreen() {
        context.getSharedPreferences(USER_INTERFACE_PREFERENCES, Context.MODE_PRIVATE)
            .edit().putBoolean(DO_NOT_ALERT_ABOUT_START_SCREEN, true).apply()
    }

    /**
     * Starts the time to show the choose start screen dialog.
     *
     * @param context   Current context.
     */
    fun setStartScreenTimeStamp() {
        context.getSharedPreferences(USER_INTERFACE_PREFERENCES, MODE_PRIVATE)
            .edit().putLong(START_SCREEN_LOGIN_TIMESTAMP, System.currentTimeMillis()).apply()
    }

    companion object {
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

        private const val TIME_TO_SHOW_START_SCREEN_DIALOG = 604800000
    }
}
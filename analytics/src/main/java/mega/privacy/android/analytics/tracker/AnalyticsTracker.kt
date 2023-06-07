package mega.privacy.android.analytics.tracker

import mega.privacy.android.analytics.event.ButtonInfo
import mega.privacy.android.analytics.event.DialogInfo
import mega.privacy.android.analytics.event.ScreenInfo
import mega.privacy.android.analytics.event.TabInfo

/**
 * Analytics tracker
 */
interface AnalyticsTracker {
    /**
     * Track screen view
     *
     * @param screen
     */
    fun trackScreenView(screen: ScreenInfo)

    /**
     * Track tab selected
     *
     * @param tab
     */
    fun trackTabSelected(tab: TabInfo)

    /**
     * Track dialog displayed
     *
     * @param dialog
     * @param screen
     */
    fun trackDialogDisplayed(dialog: DialogInfo, screen: ScreenInfo)

    /**
     * Track dialog displayed
     *
     * @param dialog
     */
    fun trackDialogDisplayed(dialog: DialogInfo)

    /**
     * Track button press
     *
     * @param button
     */
    fun trackButtonPress(button: ButtonInfo)
}
package mega.privacy.android.app.lollipop.managerSections.settings

@Deprecated(
    "Interim class to facilitate refactor of settings fragment",
    replaceWith = ReplaceWith("SettingsFragment")
)
interface Settings {
    var numberOfClicksKarere: Int
    var numberOfClicksAppVersion: Int
    var numberOfClicksSDK: Int
    var setAutoAccept: Boolean
    var autoAcceptSetting: Boolean

    fun setOnlineOptions(isOnline: Boolean)

    /**
     * Refresh the Camera Uploads service settings depending on the service status.
     */
    fun refreshCameraUploadsSettings()
    fun goToCategoryStorage()
    fun goToCategoryQR()
    fun goToSectionStartScreen()

    /**
     * Method for controlling whether or not to display the action bar elevation.
     */
    fun checkScroll()

    /**
     * Scroll to the beginning of Settings page.
     * In this case, the beginning is category KEY_FEATURES.
     *
     *
     * Note: If the first category changes, this method should be updated with the new one.
     */
    fun goToFirstCategory()
    fun update2FAVisibility()
    fun hidePreferencesChat()
    fun setValueOfAutoAccept(autoAccept: Boolean)

    /**
     * Re-enable 'findPreference<SwitchPreferenceCompat>(KEY_2FA)' after 'multiFactorAuthCheck' finished.
     */
    fun reEnable2faSwitch()
    fun update2FAPreference(enabled: Boolean)

    /**
     * Update the Cancel Account settings.
     */
    fun updateCancelAccountSetting()
}
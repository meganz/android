package mega.privacy.android.app.main.managerSections.settings

@Deprecated(
    "Interim class to facilitate refactor of settings fragment",
    replaceWith = ReplaceWith("SettingsFragment")
)
interface Settings {
    var numberOfClicksKarere: Int
    var numberOfClicksAppVersion: Int
    var numberOfClicksSDK: Int
    var setAutoAccept: Boolean//Refactored
    var autoAcceptSetting: Boolean//Refactored

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
    fun update2FAVisibility()//Refactored
    fun hidePreferencesChat()
    fun setValueOfAutoAccept(autoAccept: Boolean)//Refactored

    /**
     * Re-enable 'findPreference<SwitchPreferenceCompat>(KEY_2FA)' after 'multiFactorAuthCheck' finished.
     */
    fun reEnable2faSwitch()//Refactored
    fun update2FAPreference(enabled: Boolean)//Refactored

    /**
     * Update the Cancel Account settings.
     */
    fun updateCancelAccountSetting()
}
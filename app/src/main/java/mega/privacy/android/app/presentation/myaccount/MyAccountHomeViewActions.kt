package mega.privacy.android.app.presentation.myaccount

/**
 * Actions interface for MyAccountHomeView
 */
interface MyAccountHomeViewActions {
    /**
     * Flag to mark whether phone number dialog has been shown to the user
     */
    val isPhoneNumberDialogShown: Boolean

    /**
     * show API Server dialog menu
     */
    fun showApiServerDialog() {}

    /**
     * action when user clicks the user's avatar
     */
    fun onClickUserAvatar() {}

    /**
     * action when user clicks the edit icon beside name text
     */
    fun onEditProfile() {}

    /**
     * action when user clicks the usage meter
     */
    fun onClickUsageMeter() {}

    /**
     * action when user clicks "Upgrade" button
     */
    fun onUpgradeAccount() {}

    /**
     * action when clicks "Add your phone number" menu
     */
    fun onAddPhoneNumber() {}

    /**
     * show the phone number dialog
     */
    fun showPhoneNumberDialog() {}

    /**
     * action when user clicks "Back up Recovery Key" menu, will navigate to Recovery Key screen
     */
    fun onBackupRecoveryKey() {}

    /**
     * action when clicks "Contacts" menu, will navigate to Contacts screen
     */
    fun onClickContacts() {}

    /**
     * action when clicks "Achievements" menu, will navigate to Achievements screen
     */
    fun onClickAchievements() {}

    /**
     * callback to handle page scrolling
     */
    fun onPageScroll(isAtTop: Boolean) {}

    /**
     * callback to reset user message event
     */
    fun resetUserMessage() {}

    /**
     * resets Achievement's navigation event
     */
    fun resetAchievementsNavigationEvent() {}
}
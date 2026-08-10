package mega.privacy.android.app.presentation.settings.model

/**
 * Entry shown in the User interface settings section for the main navigation preference.
 */
sealed interface NavigationSettingsEntry {

    /**
     * Start screen entry, shown while the customisable bottom navigation feature is disabled.
     *
     * @property summary name of the selected start screen
     */
    data class StartScreen(val summary: String) : NavigationSettingsEntry

    /**
     * Customise navigation entry, shown while the customisable bottom navigation feature is
     * enabled.
     *
     * @property customised true when the saved arrangement differs from the default one
     */
    data class CustomiseNavigation(val customised: Boolean) : NavigationSettingsEntry
}

package mega.privacy.android.navigation.settings

/**
 * Setting click action type
 */
sealed interface SettingClickActionType {
    /**
     * Navigation action
     *
     * @property target - This needs to be a valid navigation destination from the settings graph for the feature
     */
    data class NavigationAction(val target: Any) : SettingClickActionType

    /**
     * Function action
     *
     * @property function
     */
    data class FunctionAction(val function: suspend () -> Unit)
}
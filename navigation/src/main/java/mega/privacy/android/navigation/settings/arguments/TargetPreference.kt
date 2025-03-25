package mega.privacy.android.navigation.settings.arguments

/**
 * Target preference
 *
 * @property preferenceId
 * @property requiresNavigation
 */
interface TargetPreference {
    val preferenceId: String
    val requiresNavigation: Boolean
}


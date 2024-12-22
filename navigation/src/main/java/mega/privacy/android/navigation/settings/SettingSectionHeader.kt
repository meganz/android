package mega.privacy.android.navigation.settings

/**
 * Setting section header
 */
sealed interface SettingSectionHeader {
    /**
     * Appearance
     */
    data object Appearance : SettingSectionHeader

    /**
     * Features
     */
    data object Features : SettingSectionHeader

    /**
     * Storage
     */
    data object Storage : SettingSectionHeader

    /**
     * User interface
     */
    data object UserInterface : SettingSectionHeader

    /**
     * Media
     */
    data object Media : SettingSectionHeader

    /**
     * Security
     */
    data object Security : SettingSectionHeader

    /**
     * Help
     */
    data object Help : SettingSectionHeader

    /**
     * About
     */
    data object About : SettingSectionHeader

    /**
     * Custom
     *
     * @property name
     */
    data class Custom(val name: String) : SettingSectionHeader
}
package mega.privacy.android.navigation.settings

/**
 * Setting entry point
 *
 * @property section
 * @property items
 */
data class SettingEntryPoint(
    val section: SettingSectionHeader,
    val items: List<SettingItem>
)
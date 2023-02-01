package mega.privacy.android.app.fragments.settingsFragments.download.model

/**
 * Download Settings UI State data class holder
 * @param downloadLocationPath as [String]
 * @param isAskAlwaysChecked as [Boolean]
 */
data class DownloadSettingsState(
    val downloadLocationPath: String? = null,
    val isAskAlwaysChecked: Boolean = true,
)
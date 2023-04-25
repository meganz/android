package mega.privacy.android.app.presentation.versions.model

/**
 * Data class representing the UI State for [mega.privacy.android.app.main.VersionsFileActivity]
 *
 * @property isNodeInBackups Checks whether the Node is a Backup Node or not
 */
data class VersionsFileState(
    val isNodeInBackups: Boolean = false,
)
